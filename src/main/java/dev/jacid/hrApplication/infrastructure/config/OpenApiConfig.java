package dev.jacid.hrApplication.infrastructure.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI document served at {@code /v3/api-docs} and committed as {@code docs/openapi.json}, the contract the
 * HRWebApp-UI generates its TypeScript types from (see ADR 0006). Everything here must be deterministic:
 * {@code OpenApiContractTest} compares the generated document with the committed file.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    /** Version of the HTTP contract; bump it together with the Maven project version on breaking changes. */
    public static final String API_VERSION = "2.0.0";

    static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI hrOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HRWebApp API")
                        .version(API_VERSION)
                        .description("""
                                Peer recognition and HR insights. Employees send (optionally anonymous) feedback to \
                                colleagues; managers see aggregated insights and control the AI sentiment analysis.

                                Every endpoint needs a Keycloak access token (`Authorization: Bearer <token>`) of the \
                                `hr-realm` realm. Roles come from the realm roles `MANAGER` and `EMPLOYEE`. \
                                Errors have the body `{"code": "...", "message": "..."}`, except 401 and \
                                role-based 403 responses, which are produced by Spring Security without a body.""")
                        .license(new License().name("MIT").url("https://github.com/jacidProgrammer/HRWebApp/blob/main/LICENSE")))
                .addServersItem(new Server().url("http://localhost:8080").description("Local development"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token issued by Keycloak (realm hr-realm, client hr-api-login)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Adds the responses every operation shares (all of them need a token and a role), marks every property of the
     * response schemas as required (the API always serialises every field, using {@code null} for missing values,
     * so generated clients get non-optional, possibly nullable, fields) and sorts the tags.
     */
    @Bean
    OpenApiCustomizer commonResponsesCustomizer() {
        return openApi -> {
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                responses.addApiResponse("401", new ApiResponse()
                        .description("Missing, invalid or expired access token (no body)"));
                responses.computeIfAbsent("403", code -> new ApiResponse()
                        .description("The caller's role is not allowed to use this endpoint (no body)"));
            }));
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas != null) {
                schemas.forEach((name, schema) -> {
                    if (isResponseSchema(name) && schema.getProperties() != null) {
                        schema.setRequired(new ArrayList<>(new TreeSet<>(schema.getProperties().keySet())));
                    }
                });
            }
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparing(Tag::getName));
            }
        };
    }

    private static boolean isResponseSchema(String name) {
        return name.equals("ErrorResponse") || (name.endsWith("DTO") && !name.endsWith("RequestDTO"));
    }
}
