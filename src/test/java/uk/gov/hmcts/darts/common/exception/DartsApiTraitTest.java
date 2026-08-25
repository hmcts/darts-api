package uk.gov.hmcts.darts.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DartsApiTraitTest {

    private static final String REQUEST_URI = "/darts-api-trait";

    private final DartsApiTrait trait = new TraitImplementation();
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void givenDartsApiExceptionWithDetail_whenHandled_thenReturnsProblemDetailWithInstance() {
        DartsApiException exception = new DartsApiException(TraitError.TEST_ERROR, "Some descriptive details");

        ResponseEntity<ProblemDetail> response = trait.handleDartsApiException(exception, webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(response.getBody()).satisfies(problemDetail -> {
            assertThat(problemDetail.getType()).isEqualTo(URI.create(TraitError.TEST_ERROR.getType()));
            assertThat(problemDetail.getTitle()).isEqualTo(TraitError.TEST_ERROR.getTitle());
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Some descriptive details");
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
        });
    }

    @Test
    void givenDartsApiExceptionWithCustomProperties_whenContentCreated_thenCopiesProperties() {
        DartsApiException exception = new DartsApiException(
            TraitError.TEST_ERROR,
            "Some descriptive details",
            Map.of("field", "name", "reason", "invalid")
        );

        ProblemDetail problemDetail = DartsApiTrait.getContentForException(exception);

        assertThat(problemDetail.getType()).isEqualTo(URI.create(TraitError.TEST_ERROR.getType()));
        assertThat(problemDetail.getTitle()).isEqualTo(TraitError.TEST_ERROR.getTitle());
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Some descriptive details");
        assertThat(problemDetail.getProperties())
            .containsEntry("field", "name")
            .containsEntry("reason", "invalid");
    }

    @Test
    void givenDartsApiExceptionWithoutCustomProperties_whenContentCreated_thenDoesNotSetProperties() {
        DartsApiException exception = new DartsApiException(TraitError.TEST_ERROR);

        ProblemDetail problemDetail = DartsApiTrait.getContentForException(exception);

        assertThat(problemDetail.getProperties()).isNull();
    }

    @Test
    void givenNativeWebRequestWithoutServletRequest_whenDartsApiExceptionHandled_thenReturnsProblemDetailWithoutInstance() {
        NativeWebRequest request = mock(NativeWebRequest.class);
        DartsApiException exception = new DartsApiException(TraitError.TEST_ERROR);

        ResponseEntity<ProblemDetail> response = trait.handleDartsApiException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(response.getBody()).satisfies(problemDetail -> assertThat(problemDetail.getInstance()).isNull());
    }

    @Test
    void givenInactiveUserException_whenChecked_thenReturnsTrue() {
        DartsApiException exception = new DartsApiException(AuthorisationError.USER_NOT_ACTIVE);

        assertThat(DartsApiTrait.isInactiveUserException(exception)).isTrue();
    }

    @Test
    void givenNonDartsException_whenCheckedForInactiveUser_thenReturnsFalse() {
        assertThat(DartsApiTrait.isInactiveUserException(new RuntimeException("not a DARTS exception"))).isFalse();
    }

    @Test
    void givenDartsExceptionWithoutError_whenCheckedForInactiveUser_thenReturnsFalse() {
        DartsApiException exception = mock(DartsApiException.class);

        assertThat(DartsApiTrait.isInactiveUserException(exception)).isFalse();
    }

    @Test
    void givenInactiveUserException_whenErrorResponseWritten_thenWritesForbiddenProblemJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        DartsApiException exception = new DartsApiException(AuthorisationError.USER_NOT_ACTIVE);

        DartsApiTrait.writeErrorResponse(response, objectMapper, exception);

        JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(responseBody.get("type").asText()).isEqualTo(AuthorisationError.USER_NOT_ACTIVE.getType());
        assertThat(responseBody.get("title").asText()).isEqualTo(AuthorisationError.USER_NOT_ACTIVE.getTitle());
        assertThat(responseBody.get("status").asInt()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenNonDartsException_whenErrorResponseWritten_thenWritesUnauthorisedFallbackProblemJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        DartsApiTrait.writeErrorResponse(response, objectMapper, new RuntimeException("not authenticated"));

        JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(responseBody.get("type").asText()).isEqualTo(AuthorisationError.USER_DETAILS_INVALID.getType());
        assertThat(responseBody.get("title").asText()).isEqualTo(AuthorisationError.USER_DETAILS_INVALID.getTitle());
        assertThat(responseBody.get("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void givenProblemDetail_whenSerialized_thenWritesRfc9457Json() throws Exception {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Bad input");
        problemDetail.setType(URI.create(TraitError.TEST_ERROR.getType()));
        problemDetail.setTitle(TraitError.TEST_ERROR.getTitle());

        String json = DartsApiTrait.getJsonForProblem(objectMapper, problemDetail);

        JsonNode responseBody = objectMapper.readTree(json);
        assertThat(responseBody.get("type").asText()).isEqualTo(TraitError.TEST_ERROR.getType());
        assertThat(responseBody.get("title").asText()).isEqualTo(TraitError.TEST_ERROR.getTitle());
        assertThat(responseBody.get("status").asInt()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(responseBody.get("detail").asText()).isEqualTo("Bad input");
    }

    private static ServletWebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest("GET", REQUEST_URI));
    }

    private static final class TraitImplementation implements DartsApiTrait {
    }

    @Getter
    @RequiredArgsConstructor
    private enum TraitError implements DartsApiError {
        TEST_ERROR(
            "TEST_999",
            HttpStatus.I_AM_A_TEAPOT,
            "A descriptive title"
        );

        private static final String ERROR_TYPE_PREFIX = "TEST";

        private final String errorTypeNumeric;
        private final HttpStatus httpStatus;
        private final String title;

        @Override
        public String getErrorTypePrefix() {
            return ERROR_TYPE_PREFIX;
        }
    }
}
