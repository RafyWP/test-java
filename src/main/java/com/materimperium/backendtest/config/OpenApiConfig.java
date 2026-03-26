package com.materimperium.backendtest.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MaterImperium Java Backend API",
                version = "v1",
                description = "API para upload, processamento assincrono e consulta de arquivos do teste tecnico.",
                contact = @Contact(name = "Test RafyWP")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "Token",
        in = SecuritySchemeIn.HEADER,
        description = "Use um dos tokens fixos de teste: token-envio, token-consulta ou token-full."
)
public class OpenApiConfig {
}
