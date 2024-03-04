package org.codesystem.server.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Bearer-Token",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi groupAgentCommunication() {
        return GroupedOpenApi.builder().addOpenApiCustomizer(openApiCustomizerTagName("Agent Communication")).group("Agent Communication").build();
    }
    @Bean
    public GroupedOpenApi groupAgentRegistration() {
        return GroupedOpenApi.builder().addOpenApiCustomizer(openApiCustomizerTagName("Agent Registration")).group("Agent Registration").build();
    }
    @Bean
    public GroupedOpenApi groupManagementAgent() {
        return GroupedOpenApi.builder().addOpenApiCustomizer(openApiCustomizerTagName("Management Agent")).group("Management Agent").build();
    }
    @Bean
    public GroupedOpenApi groupManagementDeployment() {
        return GroupedOpenApi.builder().addOpenApiCustomizer(openApiCustomizerTagName("Management Deployment")).group("Management Deployment").build();
    }
    @Bean
    public GroupedOpenApi groupManagementPackage() {
        return GroupedOpenApi.builder().addOpenApiCustomizer(openApiCustomizerTagName("Management Packages")).group("Management Packages").build();
    }
    @Bean
    public GroupedOpenApi groupMonitoring() {
        return GroupedOpenApi.builder().addOpenApiCustomizer(openApiCustomizerTagName("Monitoring")).group("Monitoring").build();
    }

    private OpenApiCustomizer openApiCustomizerTagName(String tagName) {
        return openApi -> openApi.getPaths().entrySet().removeIf(path -> path.getValue().readOperations().stream().noneMatch(operation -> operation.getTags().contains(tagName)));
    }
}
