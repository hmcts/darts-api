package uk.gov.hmcts.darts.arm.client.config;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import groovy.util.logging.Slf4j;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;

@Slf4j
public class ArmClientConfig {

    @Bean
    public Encoder armTokenClientEncoder() {
        // Normally SpringFormEncoder would be used to encode form data, but this doesn't support the "text/plain" Content-Type required by ARM API. Therefore,
        // we must implement our own encoder here.
        return (o, type, requestTemplate) -> {
            if (o instanceof ArmTokenRequest armTokenRequest) {
                encodeArmTokenRequest(armTokenRequest, requestTemplate);
            } else {
                throw new EncodeException(String.format("%s is not a type supported by this encoder.", o.getClass()));
            }
        };
    }

    private void encodeArmTokenRequest(ArmTokenRequest armTokenRequest, RequestTemplate requestTemplate) {
        requestTemplate.body(String.format(
                                 "&username=%s&password=%s",
                                 armTokenRequest.getUsername(),
                                 armTokenRequest.getPassword()
                             )
        );
    }

}
