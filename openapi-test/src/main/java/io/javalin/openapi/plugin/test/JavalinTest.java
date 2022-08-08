package io.javalin.openapi.plugin.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiName;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiPropertyType;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;
import io.javalin.openapi.plugin.ApiKeyAuth;
import io.javalin.openapi.plugin.BasicAuth;
import io.javalin.openapi.plugin.BearerAuth;
import io.javalin.openapi.plugin.CookieAuth;
import io.javalin.openapi.plugin.ImplicitFlow;
import io.javalin.openapi.plugin.OAuth2;
import io.javalin.openapi.plugin.OpenApiConfiguration;
import io.javalin.openapi.plugin.OpenApiContact;
import io.javalin.openapi.plugin.OpenApiInfo;
import io.javalin.openapi.plugin.OpenApiLicense;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.OpenID;
import io.javalin.openapi.plugin.Security;
import io.javalin.openapi.plugin.SecurityConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;

/**
 * Starts Javalin server with OpenAPI plugin
 */
public final class JavalinTest implements Handler {

    /**
     * Runs server on localhost:8080
     *
     * @param args args
     */
    public static void main(String[] args) {
        Javalin.create(config -> {
            String deprecatedDocsPath = "/swagger-docs";

            OpenApiContact openApiContact = new OpenApiContact();
            openApiContact.setName("API Support");
            openApiContact.setUrl("https://www.example.com/support");
            openApiContact.setEmail("support@example.com");

            OpenApiLicense openApiLicense = new OpenApiLicense();
            openApiLicense.setName("Apache 2.0");
            openApiLicense.setIdentifier("Apache-2.0");

            OpenApiInfo openApiInfo = new OpenApiInfo();
            openApiInfo.setTitle("Awesome App");
            openApiInfo.setSummary("App summary");
            openApiInfo.setDescription("App description goes right here");
            openApiInfo.setTermsOfService("https://example.com/tos");
            openApiInfo.setContact(openApiContact);
            openApiInfo.setLicense(openApiLicense);
            openApiInfo.setVersion("1.0.0");

            OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
            openApiConfiguration.setInfo(openApiInfo);
            openApiConfiguration.setDocumentationPath(deprecatedDocsPath); // by default it's /openapi
            // Based on official example: https://swagger.io/docs/specification/authentication/oauth2/
            openApiConfiguration.setSecurity(new SecurityConfiguration(
                    Map.ofEntries(
                        entry("BasicAuth", new BasicAuth()),
                        entry("BearerAuth", new BearerAuth()),
                        entry("ApiKeyAuth", new ApiKeyAuth()),
                        entry("CookieAuth", new CookieAuth("JSESSIONID")),
                        entry("OpenID", new OpenID("https://example.com/.well-known/openid-configuration")),
                        entry("OAuth2", new OAuth2(
                                "This API uses OAuth 2 with the implicit grant flow.",
                                List.of(
                                        new ImplicitFlow(
                                                "https://api.example.com/oauth2/authorize",
                                                new HashMap<>() {{
                                                    put("read_pets", "read your pets");
                                                    put("write_pets", "modify pets in your account");
                                                }}
                                        )
                                )
                        ))
                    ),
                    List.of(
                            new Security(
                                    "oauth2",
                                    List.of(
                                            "write_pets",
                                            "read_pets"
                                    )
                            )
                    )
            ));
            openApiConfiguration.setDocumentProcessor(docs -> { // you can add whatever you want to this document using your favourite json api
                docs.set("test", new TextNode("Value"));
                return docs.toPrettyString();
            });
            config.plugins.register(new OpenApiPlugin(openApiConfiguration));

            SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
            swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
            config.plugins.register(new SwaggerPlugin(swaggerConfiguration));

            ReDocConfiguration reDocConfiguration = new ReDocConfiguration();
            reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
            config.plugins.register(new ReDocPlugin(reDocConfiguration));
        })
        .start(8080);
    }

    private static final String ROUTE = "/main/{name}";

    @Override
    @OpenApi(
            path = ROUTE,
            operationId = "cli",
            methods = HttpMethod.POST,
            summary = "Remote command execution",
            description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
            tags = { "Cli" },
            security = {
                    @OpenApiSecurity(name = "BasicAuth")
            },
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = String.class),
                            @OpenApiContent(from = EntityDto[].class)
                    }
            ),
            headers = {
                    //@OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = UUID.class),
                    @OpenApiParam(name = "Optional"),
                    @OpenApiParam(name = "X-Rick", example = "Rolled"),
                    @OpenApiParam(name = "X-SomeNumber", required = true, type = Integer.class, example = "500")
            },
            pathParams = {
                    @OpenApiParam(name = "name", description = "Name", required = true, type = UUID.class)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Status of the executed command", content = {
                            @OpenApiContent(from = EntityDto[].class)
                    }),
                    @OpenApiResponse(
                            status = "400",
                            description = "Error message related to the invalid command format (0 < command length < " + 10 + ")",
                            content = @OpenApiContent(from = EntityDto[].class)
                    ),
                    @OpenApiResponse(status = "401", description = "Error message related to the unauthorized access", content = {
                            @OpenApiContent(from = EntityDto[].class)
                    })
            }
    )
    public void handle(@NotNull Context ctx) { }

    static final class EntityDto implements Serializable {

        private final int status;
        private final String message;
        private final String timestamp;
        private final Foo foo;
        private final List<Foo> foos;
        private Bar bar;

        public EntityDto(int status, String message, Foo foo, List<Foo> foos, Bar bar) {
            this.status = status;
            this.message = message;
            this.foo = foo;
            this.foos = foos;
            this.bar = bar;
            this.timestamp = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
        }

        // should ignore
        public void setBar(Bar bar) {
            this.bar = bar;
        }

        // should be displayed as standard json section
        public Bar getBar() {
            return bar;
        }

        // should be represented by array
        public List<Foo> getFoos() {
            return foos;
        }

        // should be displayed as string
        @OpenApiPropertyType(definedBy = String.class)
        public Foo getFoo() {
            return foo;
        }

        // should support primitive types
        public int getStatus() {
            return status;
        }

        // should rename
        @OpenApiName("message")
        public String getMessageValue() {
            return message;
        }

        // should ignore
        @OpenApiIgnore
        public String getFormattedMessage() {
            return status + message;
        }

        // should contain example
        @OpenApiExample("2022-08-14T21:13:03.546Z")
        public String getTimestamp() {
            return timestamp;
        }

        // should contain example for primitive types, SwaggerUI will automatically display this as an Integer
        @OpenApiExample("5050")
        public int getVeryImportantNumber() {
            return status + 1;
        }
    }

    static final class Foo {

        private String property;

        private String link;

        public String getProperty() {
            return property;
        }

        @OpenApiExample("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        public String getLink() {
            return link;
        }

    }

    static final class Bar {

        private String property;

        public String getProperty() {
            return property;
        }

    }

}
