package org.codesystem.server.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class CorsConfiguration implements WebMvcConfigurer {
    @Value("${cors.whitelisting.url}")
    private String corsWhitelistingUrl;

    /**
     * This Method will add the URls from the corsWhitelistingUrl-Variable to the allowed URLs.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowedOriginPatterns(corsWhitelistingUrl)
                .allowCredentials(true)
                .maxAge(-1);
    }
}
