package uk.gov.hmcts.darts.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ErrorResponseAdviceTraitTest implements ErrorResponseAdviceTrait {

    @Mock
    NativeWebRequest nativeRequest;

    @Test
    void shouldProduceProblemResponseFromErrorResponseException() {

        var exampleErrorResponseException = new NoResourceFoundException(HttpMethod.GET, "not-existing-resource");

        var responseEntity = handleErrorResponseException(exampleErrorResponseException, exampleErrorResponseException, nativeRequest);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody().getTitle()).isEqualTo("Not Found");
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("No static resource not-existing-resource.");
    }
}
