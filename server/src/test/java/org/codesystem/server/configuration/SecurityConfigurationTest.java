package org.codesystem.server.configuration;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.ServerRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityConfigurationTest {
    private static MariaDB4jSpringService DB;
    @MockBean
    ServerInitialization serverInitialization;

    @Autowired
    ServerRepository serverRepository;

    @LocalServerPort
    int serverPort;

    @BeforeAll
    public static void init() {
        DB = new MariaDB4jSpringService();
        DB.setDefaultPort(3307);
        DB.setDefaultOsUser("root");
        DB.start();
    }

    @AfterAll
    public static void cleanupDB() {
        DB.stop();
    }

    @AfterEach()
    void tearDown() {
        serverRepository.deleteAll();
    }

    @Test
    void securityFilterChainSwagger() throws IOException {
        URL url = new URL("http://localhost:" + serverPort + "/swagger-ui/index.html");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        Assertions.assertEquals(403, connection.getResponseCode());

        url = new URL("http://localhost:" + serverPort + "/v3/api-docs");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        Assertions.assertEquals(403, connection.getResponseCode());
    }

    @Test
    void securityFilterChainMonitoring() throws IOException {
        URL url = new URL("http://127.0.0.1:" + serverPort + "/monitoring/health");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        Assertions.assertEquals(200, connection.getResponseCode());
    }

    @Test
    void securityFilterChainAgentCommunication() throws IOException {
        URL url = new URL("http://127.0.0.1:" + serverPort + "/api/agent/communication/checkForUpdates");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.connect();
        Assertions.assertEquals(400, connection.getResponseCode());

        url = new URL("http://127.0.0.1:" + serverPort + "/api/agent/registration");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.connect();
        Assertions.assertEquals(400, connection.getResponseCode());
    }

    @Test
    void securityFilterChainWebAPI() throws IOException {
        URL url = new URL("http://127.0.0.1:" + serverPort + "/api/server/registrationToken");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        Assertions.assertEquals(401, connection.getResponseCode());
    }

    @Test
    void securityFilterChainAgentDownload() throws IOException {
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setPublicKeyBase64("PublicKey");
        serverEntity.setPrivateKeyBase64("PrivateKey");
        serverEntity.setAgentChecksum("checksum");
        serverEntity.setAgentRegistrationToken("Token");
        serverRepository.save(serverEntity);

        URL url = new URL("http://127.0.0.1:" + serverPort + "/download/agent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        Assertions.assertEquals(403, connection.getResponseCode());
    }
}