package org.codesystem.utility;

import org.codesystem.PropertiesLoader;
import org.codesystem.TestSystemExitException;
import org.codesystem.enums.OperatingSystem;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockserver.model.HttpRequest.request;

class PackageUtilityTest {
    CryptoUtility cryptoUtility;
    OperatingSystem operatingSystem;
    PropertiesLoader propertiesLoader;
    DownloadUtility downloadUtility;
    PackageUtility packageUtility;
    ClientAndServer mockServer;
    MockedStatic<SystemExitUtility> systemExitMockedStatic;


    @BeforeEach
    void setUp() {
        cryptoUtility = Mockito.mock(CryptoUtility.class);
        operatingSystem = Mockito.mock(OperatingSystem.class);
        propertiesLoader = Mockito.mock(PropertiesLoader.class);
        downloadUtility = Mockito.mock(DownloadUtility.class);
        packageUtility = new PackageUtility(cryptoUtility, operatingSystem, propertiesLoader, downloadUtility);

        Mockito.when(propertiesLoader.getProperty("Server.Url")).thenReturn("http://localhost:8899");
        Mockito.when(propertiesLoader.getProperty("Agent.ECC.Public-Key")).thenReturn("Agent PublicKey");
        Mockito.when(cryptoUtility.createSignatureECC(Mockito.any())).thenReturn("Signature".getBytes(StandardCharsets.UTF_8));
        Mockito.when(cryptoUtility.encryptECC(Mockito.any())).thenReturn("Encrypted".getBytes(StandardCharsets.UTF_8));

        mockServer = ClientAndServer.startClientAndServer(8899);
        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);
        deleteFiles();
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
        systemExitMockedStatic.close();
        deleteFiles();
    }

    private void deleteFiles() {
        Paths.get("download").toFile().delete();
    }

    @Test
    void initiateDeployment_invalid() {
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(400));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());

        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("invalidChecksum");
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());

        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());

        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file.zip")).thenReturn("invalidChecksum");
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true).then(invocationOnMock -> {
            Paths.get("download/file.zip").toFile().createNewFile();
            return null;
        });
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());

        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file.zip")).thenReturn("checksumPlaintext");
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true).then(invocationOnMock -> {
            Paths.get("download/file.zip").toFile().createNewFile();
            return null;
        });
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());

        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file.zip")).thenReturn("checksumPlaintext");
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            ZipEntry zipEntry = new ZipEntry("test");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("download/file.zip"));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write("Test content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            return true;
        });
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());

        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file.zip")).thenReturn("checksumPlaintext");
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            ZipEntry zipEntry = new ZipEntry("test");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("download/file.zip"));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write("Test content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            Paths.get("download/extracted").toFile().mkdirs();
            Paths.get("download/extracted/start.sh").toFile().createNewFile();
            return true;
        });
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());
    }

    @Test
    void initiateDeployment_invalidSignature() {
        // valid deployment Linux
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted");
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file.zip")).thenReturn("checksumPlaintext");
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            ZipEntry zipEntry = new ZipEntry("test");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("download/file.zip"));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write("Test content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            Paths.get("download/extracted").toFile().mkdirs();
            Paths.get("download/extracted/start.sh").toFile().createNewFile();
            return true;
        });
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/deploymentResult")).respond(HttpResponse.response().withStatusCode(200));
        Assertions.assertThrows(TestSystemExitException.class, () -> packageUtility.initiateDeployment());
    }

    @Test
    void initiateDeployment_valid() {
        // valid deployment Linux
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        JSONObject jsonObject = new JSONObject().put("deploymentUUID", "deploymentUUID").put("encryptionToken", "encryptionToken").put("initializationVector", "initializationVector").put("checksumPlaintext", "checksumPlaintext").put("checksumEncrypted", "checksumEncrypted").put("signature", Base64.getEncoder().encodeToString("Signature".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(cryptoUtility.decryptECC(Mockito.any())).thenReturn(jsonObject.toString());
        Mockito.when(downloadUtility.downloadFile(Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            Paths.get("download/file").toFile().createNewFile();
            return null;
        });
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file")).thenReturn("checksumEncrypted");
        Mockito.when(cryptoUtility.calculateChecksumOfFile("download/file.zip")).thenReturn("checksumPlaintext");
        Mockito.when(cryptoUtility.verifySignatureECC(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(cryptoUtility.decryptFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).then(invocationOnMock -> {
            ZipEntry zipEntry = new ZipEntry("test");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("download/file.zip"));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write("Test content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            Paths.get("download/extracted").toFile().mkdirs();
            Paths.get("download/extracted/start.sh").toFile().createNewFile();
            return true;
        });
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/package")).respond(HttpResponse.response().withStatusCode(200).withBody(new JSONObject().put("message", Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8))).toString()));
        mockServer.when(request().withMethod("POST").withPath("/api/agent/communication/deploymentResult")).respond(HttpResponse.response().withStatusCode(200));
        Assertions.assertDoesNotThrow(() -> packageUtility.initiateDeployment());
    }
}