package org.example.rideshare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    // Adds the "Authorize" button to Swagger UI so a JWT from /api/auth/login can be pasted once
    @Bean
    public OpenAPI rideshareOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideShare API")
                        .version("1.0")
                        .description("Ride booking backend: register as ROLE_USER or ROLE_DRIVER, "
                                + "log in for a JWT, then click Authorize and paste the token."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
