package org.codesystem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codesystem.exceptions.SevereAgentErrorException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class PropertiesLoader extends Properties {
    private static final File PROPERTIES_FILE = new File("opd-agent.properties");
    private final List<String> requiredPropertiesOptions = new ArrayList<>(Arrays.asList("Server.Url", "Server.Registered", Variables.PROPERTIES_SERVER_ECC_PUBLIC_KEY, Variables.PROPERTIES_AGENT_ECC_PUBLIC_KEY, Variables.PROPERTIES_AGENT_ECC_PRIVATE_KEY, "Agent.Update-Interval"));

    @Override
    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public synchronized void load(Reader reader) {
        throw new SevereAgentErrorException("This method should not be used");
    }

    @Override
    public synchronized void load(InputStream inStream) {
        try {
            super.load(inStream);
            validatePropertiesFile();
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot load properties: " + e.getMessage());
        }
    }

    public void saveProperties() {
        try (FileWriter fileWriter = new FileWriter(PROPERTIES_FILE.getName())) {
            super.store(fileWriter, null);
        } catch (IOException e) {
            throw new SevereAgentErrorException("Cannot save properties: " + e.getMessage());
        }
    }

    public void loadProperties() {
        AgentApplication.logger.info("Loading properties file: '" + PROPERTIES_FILE.getPath() + "'");
        if (PROPERTIES_FILE.exists()) {
            InputStream inputStream;
            try {
                inputStream = new FileInputStream(PROPERTIES_FILE);
            } catch (FileNotFoundException e) {
                throw new SevereAgentErrorException("Cannot load properties-stream: " + e.getMessage());
            }
            load(inputStream);
        } else {
            AgentApplication.logger.warning("No '" + PROPERTIES_FILE.getName() + "' file was found, creating one");
            this.saveProperties();
            loadProperties();
        }
    }

    private void checkPropertiesPresentAndSet(String propertiesKey) {
        if (!this.containsKey(propertiesKey)) {
            this.setProperty(propertiesKey, "");
            this.saveProperties();
            throw new SevereAgentErrorException("The following value was not set in the properties file: '" + propertiesKey + "'");
        }
        if (this.getProperty(propertiesKey).isBlank()) {
            throw new SevereAgentErrorException("The following value was not set in the properties file: '" + propertiesKey + "'");
        }
    }

    private boolean isPropertiesPresentAndSet(String propertiesKey) {
        if (!this.containsKey(propertiesKey)) {
            this.setProperty(propertiesKey, "");
            this.saveProperties();
            return false;
        }
        return !this.getProperty(propertiesKey).isBlank();
    }


    private void validatePropertiesFile() {
        AgentApplication.logger.info("Validating properties file");
        for (String propertiesKey : requiredPropertiesOptions) {
            switch (propertiesKey) {
                case "Server.Url" -> {
                    checkPropertiesPresentAndSet(propertiesKey);
                    URL url;
                    try {
                        url = new URL(this.getProperty(propertiesKey));
                    } catch (MalformedURLException e) {
                        throw new SevereAgentErrorException("Error when parsing the Server URL: '" + e.getMessage() + "'");
                    }
                    switch (url.getProtocol()) {
                        case "https" -> {
                            //required, to not trigger warning
                        }
                        case "http" ->
                                AgentApplication.logger.warning("The HTTP protocol is used to communicate with the Server. HTTPS is highly recommended");
                        default ->
                                throw new SevereAgentErrorException("The protocol is used, but not supported, to communicate with the Server: '" + url.getProtocol() + "'");
                    }
                }

                case Variables.PROPERTIES_AGENT_ECC_PUBLIC_KEY, Variables.PROPERTIES_AGENT_ECC_PRIVATE_KEY -> {
                    if (isPropertiesPresentAndSet(Variables.PROPERTIES_AGENT_ECC_PUBLIC_KEY) && isPropertiesPresentAndSet(Variables.PROPERTIES_AGENT_ECC_PRIVATE_KEY)) {
                        //load existing Key Pair
                        KeyFactory keyFactory;
                        try {
                            //load KeyFactory and public Key
                            keyFactory = KeyFactory.getInstance("EC");
                            X509EncodedKeySpec x509EncodedKeySpecPublicKey = new X509EncodedKeySpec(Base64.getDecoder().decode(this.getProperty(Variables.PROPERTIES_AGENT_ECC_PUBLIC_KEY)));
                            keyFactory.generatePublic(x509EncodedKeySpecPublicKey);
                        } catch (Exception e) {
                            throw new SevereAgentErrorException("Unable to load the KeyFactory Algorithm: " + e.getMessage());
                        }
                        try {
                            //load private Key
                            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(this.getProperty(Variables.PROPERTIES_AGENT_ECC_PRIVATE_KEY)));
                            keyFactory.generatePrivate(pkcs8EncodedKeySpec);
                        } catch (InvalidKeySpecException e) {
                            throw new SevereAgentErrorException("Unable to load the Private-Key: " + e.getMessage());
                        }
                    } else if (!isPropertiesPresentAndSet(Variables.PROPERTIES_AGENT_ECC_PUBLIC_KEY) && !isPropertiesPresentAndSet(Variables.PROPERTIES_AGENT_ECC_PRIVATE_KEY)) {
                        //generate new Key Pair
                        AgentApplication.logger.info("No Key-Pair found. Generating Public and Private Key");
                        KeyPairGenerator keyPairGenerator;
                        try {
                            keyPairGenerator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
                        } catch (Exception e) {
                            throw new SevereAgentErrorException("Unable to load the KeyFactory Algorithm: " + e.getMessage());
                        }
                        try {
                            keyPairGenerator.initialize(new ECGenParameterSpec("sect571k1"));
                        } catch (InvalidAlgorithmParameterException e) {
                            throw new SevereAgentErrorException("Unable to load the Algorithm: " + e.getMessage());
                        }
                        KeyPair keyPair = keyPairGenerator.generateKeyPair();
                        this.setProperty(Variables.PROPERTIES_AGENT_ECC_PUBLIC_KEY, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
                        this.setProperty(Variables.PROPERTIES_AGENT_ECC_PRIVATE_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
                        saveProperties();
                        AgentApplication.logger.info("Key Pair generated and stored in Properties File");

                    } else {
                        //only one variable is set
                        throw new SevereAgentErrorException("Unable to load the Public and Private Key. Make sure both values are set. Clear both values to generate a new Key-Pair on startup");
                    }

                }

                case "Server.Registered" -> {
                    if (!isPropertiesPresentAndSet(propertiesKey)) {
                        this.setProperty(propertiesKey, "false");
                        saveProperties();
                    } else if (!this.getProperty("Server.Registered").equals("false") && !this.getProperty("Server.Registered").equals("true")) {
                        throw new SevereAgentErrorException("Only 'true' or 'false' values are allowed for: " + propertiesKey);
                    }
                }

                case Variables.PROPERTIES_SERVER_ECC_PUBLIC_KEY -> {
                    if (isPropertiesPresentAndSet(propertiesKey)) {
                        KeyFactory keyFactory;
                        try {
                            //load KeyFactory and server public Key
                            keyFactory = KeyFactory.getInstance("EC");
                            X509EncodedKeySpec x509EncodedKeySpecPublicKey = new X509EncodedKeySpec(Base64.getDecoder().decode(this.getProperty(Variables.PROPERTIES_SERVER_ECC_PUBLIC_KEY)));
                            keyFactory.generatePublic(x509EncodedKeySpecPublicKey);
                        } catch (NoSuchAlgorithmException e) {
                            throw new SevereAgentErrorException("Unable to load the KeyFactory Algorithm: " + e.getMessage());
                        } catch (InvalidKeySpecException e) {
                            throw new SevereAgentErrorException("Unable to load the Server Public-Key: " + e.getMessage());
                        }
                    }
                }

                case "Agent.Update-Interval" -> {
                    if (!isPropertiesPresentAndSet(propertiesKey)) {
                        this.setProperty(propertiesKey, "60");
                        saveProperties();
                    } else {
                        try {
                            Integer.parseInt(this.getProperty(propertiesKey));
                        } catch (NumberFormatException e) {
                            AgentApplication.logger.severe(propertiesKey + " must be a valid integer");
                            System.exit(1);
                        }
                    }
                }

                default -> checkPropertiesPresentAndSet(propertiesKey);
            }
        }
        AgentApplication.logger.info("Properties file successfully validated");
    }
}

