package uk.gov.hmcts.darts.common.exception;

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
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.common.exception.GlobalExceptionHandlerTest.MockController;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void shouldReturnRfc7807ResponseWhenADartsApiExceptionIsThrown() throws Exception {
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
                "status":418
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldReturnRfc7807ResponseWithDetailFieldPopulatedWhenADartsApiExceptionIsThrownWithDetail()
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
                "detail":"Some descriptive details"
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldReturnAGenericRfc7807ResponseWhenARuntimeExceptionIsThrown() throws Exception {
        Mockito.when(mockController.test())
            .thenThrow(new RuntimeException("A runtime exception occurred"));

        MvcResult response = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "title":"Internal Server Error",
                "status":500,
                "detail":"A runtime exception occurred"
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
                "detail":"JSON parse error",
                "title":"Bad Request",
                "status":400
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

}
