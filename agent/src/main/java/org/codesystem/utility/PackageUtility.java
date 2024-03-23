package org.codesystem.utility;

import net.lingala.zip4j.ZipFile;
import okhttp3.*;
import org.codesystem.AgentApplication;
import org.codesystem.PropertiesLoader;
import org.codesystem.Variables;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.enums.PackageDeploymentErrorState;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.DeploymentResult;
import org.codesystem.payload.EmptyRequest;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.PackageDetailResponse;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.stream.Stream;

public class PackageUtility {
    private final CryptoUtility cryptoUtility;
    private final OperatingSystem operatingSystem;
    private final PropertiesLoader propertiesLoader;
    private final DownloadUtility downloadUtility;
    private PackageDetailResponse packageDetailResponse;

    public PackageUtility(CryptoUtility cryptoUtility, OperatingSystem operatingSystem, PropertiesLoader propertiesLoader, DownloadUtility downloadUtility) {
        this.cryptoUtility = cryptoUtility;
        this.operatingSystem = operatingSystem;
        this.propertiesLoader = propertiesLoader;
        this.downloadUtility = downloadUtility;
    }

    public void initiateDeployment() {
        AgentApplication.logger.info("Clean Folder");
        cleanupDownloadFolder();
        AgentApplication.logger.info("Get Package Details");
        getPackageDetails();
        AgentApplication.logger.info("Download Package");
        MediaType mediaType = Variables.MEDIA_TYPE_JSON;
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoUtility), cryptoUtility, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_URL) + "/api/agent/communication/package/" + packageDetailResponse.getDeploymentUUID())
                .post(body)
                .build();
        downloadUtility.downloadFile(Variables.PATH_PACKAGE_ENCRYPTED, request);
        AgentApplication.logger.info("Verifiy");
        if (!cryptoUtility.calculateChecksumOfFile(Variables.FILE_NAME_PACKAGE_ENCRYPTED).equals(packageDetailResponse.getChecksumEncrypted())) {
            sendDeploymentResponse(PackageDeploymentErrorState.ENCRYPTED_CHECKSUM_MISMATCH.toString());
        }
        AgentApplication.logger.info("Decrypt");
        if (!decryptPackage()) {
            sendDeploymentResponse(PackageDeploymentErrorState.DECRYPTION_FAILED.toString());
        }
        AgentApplication.logger.info("Verify");
        if (!cryptoUtility.calculateChecksumOfFile(Variables.FILE_NAME_PACKAGE_DECRYPTED).equals(packageDetailResponse.getChecksumPlaintext())) {
            sendDeploymentResponse(PackageDeploymentErrorState.PLAINTEXT_CHECKSUM_MISMATCH.toString());
        }
        AgentApplication.logger.info("extract");
        extractPackage(Variables.FILE_NAME_PACKAGE_DECRYPTED, "download/extracted");
        AgentApplication.logger.info("start deployment");
        sendDeploymentResponse(executeDeployment());
        AgentApplication.logger.info("Final cleanup");
        cleanupDownloadFolder();
    }

    private void cleanupDownloadFolder() {
        //clear download folder
        Path downloadFolder = Paths.get("download");
        if (Files.exists(downloadFolder)) {
            try (Stream<Path> stream = Files.walk(downloadFolder)) {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                throw new SevereAgentErrorException("Cannot delete download folder: " + e.getMessage());
            }
        }
        new File(downloadFolder.toAbsolutePath().toString()).mkdirs();
    }

    private void getPackageDetails() {
        MediaType mediaType = Variables.MEDIA_TYPE_JSON;
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoUtility), cryptoUtility, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder().url(propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_URL) + Variables.URL_PACKAGE_DETAIL).post(body).build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new SevereAgentErrorException("Error getting details: " + response.code());
            }
            String responseBody = new JSONObject(response.body().string()).getString("message");
            String decrypted = cryptoUtility.decryptECC(Base64.getDecoder().decode(responseBody.getBytes(StandardCharsets.UTF_8)));
            this.packageDetailResponse = new PackageDetailResponse(new JSONObject(decrypted));
        } catch (Exception e) {
            throw new SevereAgentErrorException("Cannot process Package Detail request: " + e.getMessage());
        }

    }

    private boolean decryptPackage() {
        return cryptoUtility.decryptFile(packageDetailResponse.getEncryptionToken(), packageDetailResponse.getInitializationVector(), new File(Variables.FILE_NAME_PACKAGE_ENCRYPTED), Paths.get(Variables.FILE_NAME_PACKAGE_DECRYPTED));
    }

    private void extractPackage(String zipFileLocation, String destinationFolderLocation) {
        try (ZipFile zipFile = new ZipFile(zipFileLocation)) {
            zipFile.extractAll(destinationFolderLocation);
        } catch (Exception e) {
            throw new SevereAgentErrorException("Error whe extracting package: " + e.getMessage());
        }
    }

    private void sendDeploymentResponse(String responseMessage) {
        DeploymentResult deploymentResult = new DeploymentResult(packageDetailResponse.getDeploymentUUID(), responseMessage);
        MediaType mediaType = Variables.MEDIA_TYPE_JSON;
        RequestBody body = RequestBody.create(new EncryptedMessage(deploymentResult.toJsonObject(cryptoUtility), cryptoUtility, propertiesLoader).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder().url(propertiesLoader.getProperty(Variables.PROPERTIES_SERVER_URL) + Variables.URL_DEPLOYMENT_RESULT).post(body).build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new SevereAgentErrorException("Error sending response: " + response.code());
            }
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot send Deployment Result: " + e.getMessage());
        }

    }

    private String executeDeployment() {
        switch (operatingSystem) {
            case LINUX -> {
                return executeLinuxDeployment();
            }
        }
        return "";
    }

    private String executeLinuxDeployment() {
        File file = Paths.get("download/extracted/start.sh").toFile();
        if (!file.exists()) {
            return PackageDeploymentErrorState.ENTRYPOINT_NOT_FOUND.toString();
        }
        if (!file.setExecutable(true)) {
            AgentApplication.logger.warning("Cannot set file as executable");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(file.getAbsolutePath());
        String returnValue;
        try {
            Process process = processBuilder.start();
            returnValue = String.valueOf(process.waitFor());
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return e.getMessage();
        }
        return returnValue;
    }
}
