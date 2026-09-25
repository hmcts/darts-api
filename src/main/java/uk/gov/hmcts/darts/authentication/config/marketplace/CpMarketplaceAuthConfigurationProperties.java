package uk.gov.hmcts.darts.authentication.config.marketplace;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("darts.authentication.marketplace.cp")
@Getter
@Setter
public class CpMarketplaceAuthConfigurationProperties {

    private boolean enabled;

    private String issuerUri;

    private String jwkSetUri;

    private String audience;

    private String identityClaim;

    private String expectedIdentity;

    private String roleClaim;

    private String expectedRole;

    private String serviceAccountEmail;

}
