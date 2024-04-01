package org.codesystem.server.utility;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.request.agent.AgentEncryptedRequest;
import org.codesystem.server.response.agent.AgentEncryptedResponse;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RequestUtility {
    private final AgentRepository agentRepository;
    private final CryptoUtility cryptoUtility;

    public JSONObject validateRequest(AgentEncryptedRequest agentEncryptedRequest) {
        if (agentEncryptedRequest.getPublicKeyBase64() == null || agentEncryptedRequest.getPublicKeyBase64().isBlank()
                || agentEncryptedRequest.getMessage() == null || agentEncryptedRequest.getMessage().isBlank()) {
            return null;
        }
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentEncryptedRequest.getPublicKeyBase64());
        if (agentEntity == null || !agentEntity.isRegistrationCompleted()) {
            return null;
        }
        JSONObject decryptedMessage;
        try {
            decryptedMessage = new JSONObject(cryptoUtility.decryptECC(Base64.getDecoder().decode(agentEncryptedRequest.getMessage())));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (decryptedMessage.isEmpty() || decryptedMessage.isNull(Variables.JSON_FIELD_SIGNATURE) || decryptedMessage.isNull(Variables.JSON_FIELD_TIMESTAMP)) {
            return null;
        }
        Instant messageTimestamp;
        try {
            messageTimestamp = Instant.parse(decryptedMessage.getString(Variables.JSON_FIELD_TIMESTAMP));
        } catch (DateTimeParseException e) {
            return null;
        }
        if (messageTimestamp.isAfter(Instant.now().plusSeconds(300)) || messageTimestamp.isBefore(Instant.now().minusSeconds(300))) {
            return null;
        }
        String messageSignature = decryptedMessage.getString(Variables.JSON_FIELD_SIGNATURE);
        decryptedMessage.remove(Variables.JSON_FIELD_SIGNATURE);
        if (messageSignature.isBlank()) {
            return null;
        }

        if (cryptoUtility.verifySignatureECC(decryptedMessage.toString(), messageSignature, agentEntity)) {
            return decryptedMessage;
        }
        return null;
    }

    public AgentEncryptedResponse generateAgentEncryptedResponse(JSONObject jsonObject, AgentEntity agentEntity) {
        if (jsonObject == null || jsonObject.isEmpty() || agentEntity == null) {
            return null;
        }
        jsonObject.put(Variables.JSON_FIELD_TIMESTAMP, Instant.now());
        String signature = Base64.getEncoder().encodeToString(cryptoUtility.createSignatureECC(jsonObject.toString()));
        jsonObject.put(Variables.JSON_FIELD_SIGNATURE, signature);
        String decryptedMessage = Base64.getEncoder().encodeToString(cryptoUtility.encryptECC(jsonObject.toString().getBytes(StandardCharsets.UTF_8), agentEntity));
        return new AgentEncryptedResponse(decryptedMessage);
    }
}
