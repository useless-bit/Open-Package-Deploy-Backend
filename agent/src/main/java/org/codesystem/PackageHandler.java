package org.codesystem;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import okhttp3.*;
import org.codesystem.enums.OperatingSystem;
import org.codesystem.enums.PackageDeploymentErrorState;
import org.codesystem.payload.DeploymentResult;
import org.codesystem.payload.EncryptedMessage;
import org.codesystem.payload.PackageDetailResponse;
import org.codesystem.payload.UpdateCheckRequest;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;

public class PackageHandler {
    private final CryptoHandler cryptoHandler;
    private final OperatingSystem operatingSystem;
    private PackageDetailResponse packageDetailResponse;

    public PackageHandler(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        this.cryptoHandler = new CryptoHandler();
    }

    public void initiateDeployment() {
        cleanupDownloadFolder();
        getPackageDetails();
        downloadPackage();
        if (!validatePackage("download/file", packageDetailResponse.getChecksumEncrypted())) {
            sendDeploymentResponse(PackageDeploymentErrorState.ENCRYPTED_CHECKSUM_MISMATCH.toString());
        }
        if (!decryptPackage()) {
            sendDeploymentResponse(PackageDeploymentErrorState.DECRYPTION_FAILED.toString());
        }
        if (!validatePackage("download/file.zip", packageDetailResponse.getChecksumPlaintext())) {
            sendDeploymentResponse(PackageDeploymentErrorState.PLAINTEXT_CHECKSUM_MISMATCH.toString());
        }
        extractPackage("download/file.zip", "download/extracted");
        sendDeploymentResponse(executeDeployment());
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
        RequestBody body = RequestBody.create(new EncryptedMessage(new UpdateCheckRequest().toJsonObject()).toJsonObject().toString(), mediaType);
        Request request = new Request.Builder()
                .url(AgentApplication.properties.getProperty("Server.Url") + "/api/agent/communication/package/" + packageDetailResponse.getDeploymentUUID())
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
            return;
        }

        byte[] data;
        try {
            data = response.body().bytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileOutputStream fos = new FileOutputStream("download/file")) {
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getPackageDetails() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new EncryptedMessage(new UpdateCheckRequest().toJsonObject()).toJsonObject().toString(), mediaType);
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
            return;
        }
        String responseBody = null;
        try {
            responseBody = new JSONObject(response.body().string()).getString("message");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String decrypted = cryptoHandler.decryptECC(Base64.getDecoder().decode(responseBody.getBytes(StandardCharsets.UTF_8)));
        this.packageDetailResponse = new PackageDetailResponse(new JSONObject(decrypted));
    }

    private boolean validatePackage(String file, String targetChecksum) {
        byte[] data;
        try {
            data = Files.readAllBytes(Paths.get(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String checksum = new BigInteger(1, hash).toString(16);
        return checksum.equals(targetChecksum);
    }

    private boolean decryptPackage() {
        return cryptoHandler.decryptFile(packageDetailResponse.getEncryptionToken(), packageDetailResponse.getInitializationVector(), new File("download/file"), Paths.get("download/file.zip"));
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
        RequestBody body = RequestBody.create(new EncryptedMessage(deploymentResult.toJsonObject()).toJsonObject().toString(), mediaType);
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
            return;
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
