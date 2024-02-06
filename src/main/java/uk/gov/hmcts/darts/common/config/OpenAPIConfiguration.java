package uk.gov.hmcts.darts.common.config;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;

@Configuration
@SecurityScheme(name = SECURITY_SCHEMES_BEARER_AUTH, type = HTTP, scheme = "bearer", bearerFormat = "JWT", in = HEADER)
public class OpenAPIConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
              .info(new Info().title("Modernised DARTS")
                    .description("Modernised DARTS")
                    .version("v0.0.1")
                    .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
              .externalDocs(new ExternalDocumentation()
                    .description("README")
                    .url("https://github.com/hmcts/spring-boot-template"));
    }

}
