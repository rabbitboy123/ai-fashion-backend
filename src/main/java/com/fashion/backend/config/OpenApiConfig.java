package com.fashion.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fashionShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Fashion Shop API")
                        .version("1.0.0")
                        .description("API for authentication, catalog, cart, orders, payments, and AI recommendations."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer jwtSecurityForProtectedOperations() {
        Set<String> publicPaths = Set.of(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/api/ai/recommend",
                "/api/payments/webhooks/sepay");

        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/") && !publicPaths.contains(path)) {
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth")));
            }
        });
    }
}
