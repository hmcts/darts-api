package uk.gov.hmcts.darts.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DartsApiErrorResponseWriterTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void givenInactiveUserException_whenChecked_thenReturnsTrue() {
        DartsApiException exception = new DartsApiException(AuthorisationError.USER_NOT_ACTIVE);

        assertThat(DartsApiErrorResponseWriter.isInactiveUserException(exception)).isTrue();
    }

    @Test
    void givenNonDartsException_whenCheckedForInactiveUser_thenReturnsFalse() {
        assertThat(DartsApiErrorResponseWriter.isInactiveUserException(new RuntimeException("not a DARTS exception"))).isFalse();
    }

    @Test
    void givenDartsExceptionWithoutError_whenCheckedForInactiveUser_thenReturnsFalse() {
        DartsApiException exception = mock(DartsApiException.class);

        assertThat(DartsApiErrorResponseWriter.isInactiveUserException(exception)).isFalse();
    }

    @Test
    void givenInactiveUserException_whenErrorResponseWritten_thenWritesForbiddenProblemJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secured-endpoint");
        MockHttpServletResponse response = new MockHttpServletResponse();
        DartsApiException exception = new DartsApiException(AuthorisationError.USER_NOT_ACTIVE);

        DartsApiErrorResponseWriter.writeErrorResponse(request, response, objectMapper, exception);

        JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(responseBody.get("type").asText()).isEqualTo(AuthorisationError.USER_NOT_ACTIVE.getType());
        assertThat(responseBody.get("title").asText()).isEqualTo(AuthorisationError.USER_NOT_ACTIVE.getTitle());
        assertThat(responseBody.get("status").asInt()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(responseBody.get("instance").asText()).isEqualTo("/secured-endpoint");
    }

    @Test
    void givenNonDartsException_whenErrorResponseWritten_thenWritesUnauthorisedFallbackProblemJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        DartsApiErrorResponseWriter.writeErrorResponse(response, objectMapper, new RuntimeException("not authenticated"));

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
        problemDetail.setType(URI.create("TEST"));
        problemDetail.setTitle("A descriptive title");

        String json = DartsApiErrorResponseWriter.getJsonForProblem(objectMapper, problemDetail);

        JsonNode responseBody = objectMapper.readTree(json);
        assertThat(responseBody.get("type").asText()).isEqualTo("TEST");
        assertThat(responseBody.get("title").asText()).isEqualTo("A descriptive title");
        assertThat(responseBody.get("status").asInt()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(responseBody.get("detail").asText()).isEqualTo("Bad input");
    }
}
