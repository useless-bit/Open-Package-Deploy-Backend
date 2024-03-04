package org.codesystem.server.service.agent.registration;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.codesystem.server.entity.AgentEntity;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.repository.AgentRepository;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.request.agent.registration.AgentRegistrationRequest;
import org.codesystem.server.request.agent.registration.AgentVerificationRequest;
import org.codesystem.server.response.agent.registration.AgentRegistrationResponse;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.utility.CryptoUtility;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentRegistrationService {
    private final AgentRepository agentRepository;
    private final ServerRepository serverRepository;
    private final CryptoUtility cryptoUtility;

    public ResponseEntity<ApiResponse> addNewAgent(AgentRegistrationRequest agentRegistrationRequest) {
        ServerEntity serverEntity = serverRepository.findAll().get(0);


        //todo add null check for eveything
        if (agentRegistrationRequest.getAuthenticationToken().equals(serverEntity.getAgentRegistrationToken())) {
            AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentRegistrationRequest.getPublicKeyBase64());
            if (agentEntity == null) {
                agentEntity = new AgentEntity();
            }
            if (agentEntity.isRegistrationCompleted()) {
                return ResponseEntity.badRequest().body(new ApiError("A Agent with this public key is already registered"));
            }
            agentEntity.setPublicKeyBase64(agentRegistrationRequest.getPublicKeyBase64());
            agentEntity.setValidationToken(UUID.randomUUID().toString());
            agentEntity.setName(agentRegistrationRequest.getName());
            agentEntity = agentRepository.save(agentEntity);


            String encryptedMessage = Base64.encodeBase64String(cryptoUtility.encryptECC(agentEntity.getValidationToken().getBytes(), agentEntity));
            return ResponseEntity.ok().body(new AgentRegistrationResponse(serverEntity.getPublicKeyBase64(), encryptedMessage));
        }
        return ResponseEntity.badRequest().body(new ApiError("Invalid Public-Key or Authentication-Token"));
    }

    public ResponseEntity<ApiResponse> verifyNewAgent(AgentVerificationRequest agentVerificationRequest) {
        //todo add null check for eveything
        AgentEntity agentEntity = agentRepository.findFirstByPublicKeyBase64(agentVerificationRequest.getPublicKeyBase64());
        if (agentEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Unknown error"));
        }


        String decryptedToken = cryptoUtility.decryptECC(Base64.decodeBase64(agentVerificationRequest.getVerificationToken()));
        if (decryptedToken.equals(agentEntity.getValidationToken())) {
            agentEntity.setRegistrationCompleted(true);
            agentRepository.save(agentEntity);
            return ResponseEntity.ok().body(new ApiError("Success"));
        }
        return ResponseEntity.badRequest().body(new ApiError("Unknown error"));
    }
}
