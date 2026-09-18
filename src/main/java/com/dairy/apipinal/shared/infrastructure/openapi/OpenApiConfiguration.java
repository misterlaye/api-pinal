package com.dairy.apipinal.shared.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI apiPinalOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("API Pinal")
                                .version("v1")
                                .description(
                                        "API de gestion et de pilotage d'une exploitation laitière."
                                )
                                .contact(
                                        new Contact()
                                                .name("API Pinal")
                                )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "basicAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("basic")
                                )
                );
    }
}