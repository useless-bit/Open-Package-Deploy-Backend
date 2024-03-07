package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.agent.registration.AgentRegistrationRequest;
import org.codesystem.server.request.agent.registration.AgentVerificationRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.agent.registration.AgentRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent Registration")
@RestController
@RequestMapping("/api/agent/registration")
@RequiredArgsConstructor
public class AgentRegistrationController {
    private final AgentRegistrationService agentRegistrationService;

    @PostMapping
    public ResponseEntity<ApiResponse> addNewAgent(@RequestBody AgentRegistrationRequest agentRegistrationRequest) {
        return agentRegistrationService.addNewAgent(agentRegistrationRequest);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyNewAgent(@RequestBody AgentVerificationRequest agentVerificationRequest) {
        return agentRegistrationService.verifyNewAgent(agentVerificationRequest);
    }
}
