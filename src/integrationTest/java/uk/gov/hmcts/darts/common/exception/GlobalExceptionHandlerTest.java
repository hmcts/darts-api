package uk.gov.hmcts.darts.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.common.exception.GlobalExceptionHandlerTest.MockController;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(MockController.class)
class GlobalExceptionHandlerTest extends IntegrationBase {

    private static final String ENDPOINT = "/test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MockController mockController;

    @RestController
    static class MockController {

        @GetMapping(ENDPOINT)
        public ResponseEntity<Void> test() {
            return ResponseEntity.ok()
                .build();
        }

        @PostMapping(ENDPOINT)
        public ResponseEntity<Void> testValidation(@Valid @RequestBody TestRequest request) {
            return ResponseEntity.ok()
                .build();
        }
    }

    record TestRequest(
        @Size(min = 1, max = 5)
        String name
    ) {
    }

    @Getter
    @RequiredArgsConstructor
    enum TestError implements DartsApiError {
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

    @Test
    void shouldReturnRfc9457ResponseWhenADartsApiExceptionIsThrown() throws Exception {
        Mockito.when(mockController.test())
            .thenThrow(new DartsApiException(TestError.TEST_ERROR));

        MvcResult response = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isIAmATeapot())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"TEST_999",
                "title":"A descriptive title",
                "status":418,
                "instance":"/test"
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldReturnRfc9457ResponseWithDetailFieldPopulatedWhenADartsApiExceptionIsThrownWithDetail()
        throws Exception {
        Mockito.when(mockController.test())
            .thenThrow(new DartsApiException(TestError.TEST_ERROR, "Some descriptive details"));

        MvcResult response = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isIAmATeapot())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"TEST_999",
                "title":"A descriptive title",
                "status":418,
                "detail":"Some descriptive details",
                "instance":"/test"
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldReturnAGenericRfc9457ResponseWhenARuntimeExceptionIsThrown() throws Exception {
        Mockito.when(mockController.test())
            .thenThrow(new RuntimeException("A runtime exception occurred"));

        MvcResult response = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"about:blank",
                "title":"Internal Server Error",
                "status":500,
                "detail":"A runtime exception occurred",
                "instance":"/test"
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void handleMessageNotReadableHandler_shouldReturnBadRequestProblem_whenHttpMessageNotReadableExceptionIsThrown() throws Exception {
        Mockito.when(mockController.test())
            .thenThrow(new HttpMessageNotReadableException("JSON parse error"));

        MvcResult response = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"about:blank",
                "detail":"JSON parse error",
                "title":"Bad Request",
                "status":400,
                "instance":"/test"
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldReturnRfc9457ResponseWithProblemPropertiesWhenValidationExceptionIsThrown() throws Exception {
        MvcResult response = mockMvc.perform(post(ENDPOINT)
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content("""
                                                              {
                                                                "name":"too-long"
                                                              }
                                                              """))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"COMMON_104",
                "title":"Invalid request",
                "status":400,
                "instance":"/test",
                "properties":{
                    "name":"size must be between 1 and 5"
                }
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

}
