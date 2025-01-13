package uk.gov.hmcts.darts.arm.client;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.arm.client.config.ArmClientConfig;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArmClientConfigTest {

    @Test
    void encodeShouldPopulateRequestBodyWithExpectedContentFromRequest() {
        // Given
        var armClientConfig = new ArmClientConfig();
        Encoder encoder = armClientConfig.armTokenClientEncoder();

        var armTokenRequest = ArmTokenRequest.builder().username("some-username").password("some-password").build();
        var requestTemplate = new RequestTemplate();

        // When
        encoder.encode(armTokenRequest, null, requestTemplate);

        // Then
        assertEquals("&username=some-username&password=some-password", new String(requestTemplate.body()));
    }

    @Test
    void encodeShouldThrowExceptionWhenProvidedWithUnexpectedType() {
        // Given
        var armClientConfig = new ArmClientConfig();
        Encoder encoder = armClientConfig.armTokenClientEncoder();

        var someUnsupportedType = new Object();
        var requestTemplate = new RequestTemplate();

        // When
        EncodeException exception = assertThrows(EncodeException.class, () -> encoder.encode(someUnsupportedType, null, requestTemplate));

        // Then
        assertEquals("class java.lang.Object is not a type supported by this encoder.", exception.getMessage());
    }

}
