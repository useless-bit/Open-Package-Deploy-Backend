package org.codesystem.utility;

import okhttp3.Request;
import org.codesystem.SystemExit;
import org.codesystem.TestSystemExitException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.mockserver.model.HttpRequest.request;

class DownloadUtilityTest {
    final String TEST_DOWNLOAD_FOLDER = "test-download";
    final Path downloadFolder = Paths.get(TEST_DOWNLOAD_FOLDER);
    final Path downloadFileOne = Paths.get(TEST_DOWNLOAD_FOLDER + "/file_one");
    final Path downloadFileTwo = Paths.get("file_two");

    DownloadUtility downloadUtility;
    ClientAndServer mockServer;
    MockedStatic<SystemExit> systemExitMockedStatic;


    @BeforeEach
    void setUp() throws IOException {
        downloadUtility = new DownloadUtility();
        mockServer = ClientAndServer.startClientAndServer(8899);

        systemExitMockedStatic = Mockito.mockStatic(SystemExit.class);
        systemExitMockedStatic.when(() -> SystemExit.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);

        deleteFolderWithContent();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.stop();
        systemExitMockedStatic.close();
        deleteFolderWithContent();
    }

    private void deleteFolderWithContent() throws IOException {
        downloadFileTwo.toFile().delete();
        if (Files.exists(downloadFolder)) {
            try (Stream<Path> pathStream = Files.walk(downloadFolder)) {
                pathStream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    void downloadFile() throws IOException {
        // invalid request
        mockServer.stop();
        Assertions.assertFalse(downloadUtility.downloadFile(null, null));
        Assertions.assertFalse(downloadUtility.downloadFile(downloadFileOne, null));
        Assertions.assertFalse(downloadUtility.downloadFile(null, new Request.Builder().url("http://localhost:8899").build()));
        deleteFolderWithContent();

        // server not available
        mockServer.stop();
        Assertions.assertThrows(TestSystemExitException.class, () -> downloadUtility.downloadFile(downloadFileOne, new Request.Builder().url("http://localhost:8899").build()));
        deleteFolderWithContent();

        // parent folder already present
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(400));
        downloadFileOne.toFile().getParentFile().mkdirs();
        Assertions.assertFalse(downloadUtility.downloadFile(downloadFileOne, new Request.Builder().url("http://localhost:8899").build()));
        deleteFolderWithContent();

        // file already present
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(400));
        downloadFileOne.toFile().getParentFile().mkdirs();
        downloadFileOne.toFile().createNewFile();
        Assertions.assertFalse(downloadUtility.downloadFile(downloadFileOne, new Request.Builder().url("http://localhost:8899").build()));
        deleteFolderWithContent();

        // invalid server
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(400));
        Request request = new Request.Builder()
                .url("http://localhost:8899/download/test")
                .build();
        Assertions.assertFalse(downloadUtility.downloadFile(downloadFileOne, request));
        Assertions.assertFalse(Files.exists(downloadFileOne));
        deleteFolderWithContent();


        // valid
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(200).withBody("Test-Content".getBytes(StandardCharsets.UTF_8)));
        request = new Request.Builder()
                .url("http://localhost:8899/download/test")
                .build();
        Assertions.assertTrue(downloadUtility.downloadFile(downloadFileOne, request));
        Assertions.assertTrue(Files.exists(downloadFileOne));
        Assertions.assertArrayEquals(Files.readAllBytes(downloadFileOne), "Test-Content".getBytes(StandardCharsets.UTF_8));
        deleteFolderWithContent();

        // valid
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(200).withBody("Test-Content".getBytes(StandardCharsets.UTF_8)));
        request = new Request.Builder()
                .url("http://localhost:8899/download/test")
                .build();
        Assertions.assertTrue(downloadUtility.downloadFile(downloadFileTwo, request));
        Assertions.assertTrue(Files.exists(downloadFileTwo));
        Assertions.assertArrayEquals(Files.readAllBytes(downloadFileTwo), "Test-Content".getBytes(StandardCharsets.UTF_8));
        deleteFolderWithContent();

        // file already present
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(200).withBody("Test-Content".getBytes(StandardCharsets.UTF_8)));
        request = new Request.Builder()
                .url("http://localhost:8899/download/test")
                .build();
        Assertions.assertTrue(downloadUtility.downloadFile(downloadFileOne, request));
        Assertions.assertTrue(Files.exists(downloadFileOne));
        Assertions.assertArrayEquals(Files.readAllBytes(downloadFileOne), "Test-Content".getBytes(StandardCharsets.UTF_8));
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(8899);
        mockServer.when(request().withMethod("GET").withPath("/download/test")).respond(HttpResponse.response().withStatusCode(200).withBody("Test-new-Content".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertFalse(downloadUtility.downloadFile(downloadFileOne, request));
        Assertions.assertTrue(Files.exists(downloadFileOne));
    }
}