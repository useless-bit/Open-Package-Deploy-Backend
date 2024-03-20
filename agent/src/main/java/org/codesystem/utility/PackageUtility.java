package org.codesystem.utility;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import okhttp3.*;
import org.codesystem.AgentApplication;
import org.codesystem.PropertiesLoader;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.enums.PackageDeploymentErrorState;
import org.codesystem.exceptions.SevereAgentErrorException;
import org.codesystem.payload.DeploymentResult;
import org.codesystem.payload.EmptyRequest;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.PackageDetailResponse;
import org.codesystem.utility.CryptoUtility;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Comparator;

public class PackageUtility {
    private final CryptoUtility cryptoUtility;
    private final OperatingSystem operatingSystem;
    private PackageDetailResponse packageDetailResponse;

    public PackageUtility(OperatingSystem operatingSystem, PropertiesLoader propertiesLoader) {
        this.operatingSystem = operatingSystem;
        this.cryptoUtility = new CryptoUtility(propertiesLoader);
    }

    public void initiateDeployment() {
        AgentApplication.logger.info("Clean Folder");
        cleanupDownloadFolder();
        AgentApplication.logger.info("Get Package Details");
        getPackageDetails();
        AgentApplication.logger.info("Download Package");
        downloadPackage();
        AgentApplication.logger.info("Verifiy");
        if (!validatePackage("download/file", packageDetailResponse.getChecksumEncrypted())) {
            sendDeploymentResponse(PackageDeploymentErrorState.ENCRYPTED_CHECKSUM_MISMATCH.toString());
        }
        AgentApplication.logger.info("Decrypt");
        if (!decryptPackage()) {
            sendDeploymentResponse(PackageDeploymentErrorState.DECRYPTION_FAILED.toString());
        }
        AgentApplication.logger.info("Verify");
        if (!validatePackage("download/file.zip", packageDetailResponse.getChecksumPlaintext())) {
            sendDeploymentResponse(PackageDeploymentErrorState.PLAINTEXT_CHECKSUM_MISMATCH.toString());
        }
        AgentApplication.logger.info("extract");
        extractPackage("download/file.zip", "download/extracted");
        AgentApplication.logger.info("start deployment");
        sendDeploymentResponse(executeDeployment());
        AgentApplication.logger.info("Final cleanup");
        cleanupDownloadFolder();
    }

    private void cleanupDownloadFolder() {
        //clear download folder
        Path downloadFolder = Paths.get("download");
        if (Files.exists(downloadFolder)) {
            try {
                Files.walk(downloadFolder)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        new File(downloadFolder.toAbsolutePath().toString()).mkdir();
    }

    private void downloadPackage() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoUtility), cryptoUtility, AgentApplication.properties).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/package/" + packageDetailResponse.getDeploymentUUID())
                .post(body)
                .build();

        Response response;
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to send response: " + e.getMessage());
        }
        if (response.code() != 200) {
            throw new RuntimeException("Unable to download package. Response code: " + response.code());
        }

        byte[] data;
        try {
            data = response.body().bytes();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to load package from response: " + e.getMessage());
        }
        try (FileOutputStream fos = new FileOutputStream("download/file")) {
            fos.write(data);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Unable to write downloaded package to file: " + e.getMessage());
        }
    }

    private void getPackageDetails() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new EmptyRequest().toJsonObject(cryptoUtility), cryptoUtility, AgentApplication.properties).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/package")
                .post(body)
                .build();

        Response response;
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response.code() != 200) {
            throw new RuntimeException("Error getting details: " + response.code());
        }
        String responseBody = null;
        try {
            responseBody = new JSONObject(response.body().string()).getString("message");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String decrypted = cryptoUtility.decryptECC(Base64.getDecoder().decode(responseBody.getBytes(StandardCharsets.UTF_8)));
        this.packageDetailResponse = new PackageDetailResponse(new JSONObject(decrypted));
    }

    private boolean validatePackage(String file, String targetChecksum) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {

            MessageDigest messageDigest = MessageDigest.getInstance("SHA3-512");

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }

            byte[] checkSum = messageDigest.digest();

            StringBuilder stringBuilder = new StringBuilder(checkSum.length * 2);
            for (byte b : checkSum) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString().equals(targetChecksum);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private boolean decryptPackage() {
        return cryptoUtility.decryptFile(packageDetailResponse.getEncryptionToken(), packageDetailResponse.getInitializationVector(), new File("download/file"), Paths.get("download/file.zip"));
    }

    private void extractPackage(String zipFileLocation, String destinationFolderLocation) {
        ZipFile zipFile = new ZipFile(zipFileLocation);
        try {
            zipFile.extractAll(destinationFolderLocation);
        } catch (ZipException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendDeploymentResponse(String responseMessage) {
        DeploymentResult deploymentResult = new DeploymentResult(packageDetailResponse.getDeploymentUUID(), responseMessage);
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(deploymentResult.toJsonObject(cryptoUtility), cryptoUtility, AgentApplication.properties).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/deploymentResult")
                .post(body)
                .build();

        Response response;
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (response.code() != 200) {
            throw new RuntimeException("Error sending response: " + response.code());
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
        file.setExecutable(true);
        ProcessBuilder processBuilder = new ProcessBuilder(file.getAbsolutePath());
        String returnValue;
        try {
            Process process = processBuilder.start();
            returnValue = String.valueOf(process.waitFor());
        } catch (InterruptedException | IOException e) {
            return e.getMessage();
        }
        return returnValue;
    }
}
