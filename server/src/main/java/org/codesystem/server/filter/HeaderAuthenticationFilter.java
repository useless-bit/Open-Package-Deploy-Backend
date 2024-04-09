package org.codesystem.server.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.ServerEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.repository.ServerRepository;
import org.codesystem.server.service.server.LogService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    private final ServerRepository serverRepository;
    private final LogService logService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authentication");
        ServerEntity serverEntity = serverRepository.findAll().get(0);
        if (auth != null && auth.equals(serverEntity.getAgentRegistrationToken())) {
            filterChain.doFilter(request, response);
        }
        logService.addEntry(Severity.WARNING, "Invalid Authentication-Token provided for: " + request.getRequestURL() + " by: " + request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
