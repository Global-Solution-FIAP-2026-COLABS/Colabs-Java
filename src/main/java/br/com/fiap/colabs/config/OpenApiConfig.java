package br.com.fiap.colabs.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Colabs API",
        version = "1.0",
        description = "Rede solidária: cidadãos doam para pontos de coleta, com triagem de urgência por IA (Spring AI) "
                + "e eventos assíncronos via RabbitMQ. Global Solution FIAP 2026/2 - CitizenTech."
))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Informe o token obtido em POST /auth/login"
)
public class OpenApiConfig {
}