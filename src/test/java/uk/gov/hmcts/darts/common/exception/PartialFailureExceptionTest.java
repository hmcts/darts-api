package uk.gov.hmcts.darts.common.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

class PartialFailureExceptionTest {

    @Test
    void testJsonParse() {
        PartialFailureException ex = PartialFailureException.getPartialPayloadJson(TranscriptionApiError.FAILED_TO_UPDATE_TRANSCRIPTIONS, new DummyJson());
        Assertions.assertEquals("{\"name\":\"test\"}", ex.getCustomProperties().get("partial_failure"));
    }

    @Test
    void testJsonParseProblem() {
        Object mockItem = Mockito.mock(Object.class);
        Mockito.when(mockItem.toString()).thenReturn(mockItem.getClass().getName());
        PartialFailureException ex = PartialFailureException.getPartialPayloadJson(TranscriptionApiError.FAILED_TO_UPDATE_TRANSCRIPTIONS, mockItem);
        Assertions.assertEquals("Error occurred marshalling payload", ex.getCustomProperties().get("partial_failure"));
    }

    class DummyJson {

        private String name = "test";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
