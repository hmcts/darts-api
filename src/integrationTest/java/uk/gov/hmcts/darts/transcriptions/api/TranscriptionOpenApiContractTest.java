package uk.gov.hmcts.darts.transcriptions.api;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TranscriptionOpenApiContractTest {

    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            TranscriptionOpenApiContractTest.class
                .getResource("/openapi/transcriptions.yaml")
                .toExternalForm()
        ).build();

    @Test
    void negativeTranscriptionId() {
        Request request = SimpleRequest.Builder
            .get("/transcriptions/-1/document")
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertTrue(report.hasErrors());
        report.getMessages().forEach(m -> log.info(m.getMessage()));
    }
}
