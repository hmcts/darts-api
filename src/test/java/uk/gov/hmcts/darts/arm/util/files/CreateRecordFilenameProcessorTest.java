package uk.gov.hmcts.darts.arm.util.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateRecordFilenameProcessorTest {

    @Test
    void givenCreateRecordFilenameProcessorValidateFilename() {
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        CreateRecordFilenameProcessor createRecordFilenameProcessor = new CreateRecordFilenameProcessor(createRecordFilename);

        assertEquals("6a374f19a9ce7dc9cc480ea8d4eca0fb", createRecordFilenameProcessor.getHashcode());
        assertEquals("a17b9015-e6ad-77c5-8d1e-13259aae1895", createRecordFilenameProcessor.getHashcode2());
        assertEquals("1", createRecordFilenameProcessor.getStatus());
    }

    @Test
    void givenCreateRecordFilenameProcessorWithInvalidFileExtensionThrowsException() {
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1";

        assertThrows(IllegalArgumentException.class, () ->
              new CreateRecordFilenameProcessor(createRecordFilename));
    }

    @Test
    void givenCreateRecordFilenameProcessorWithInvalidFilenameThrowsException() {
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fba17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";

        assertThrows(IllegalArgumentException.class, () ->
              new CreateRecordFilenameProcessor(createRecordFilename));
    }
}
