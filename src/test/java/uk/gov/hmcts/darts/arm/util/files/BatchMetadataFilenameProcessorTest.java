package uk.gov.hmcts.darts.arm.util.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchMetadataFilenameProcessorTest {

    @Test
    void givenBatchMetadataFilenameProcessorValidateFilename() {
        // given
        String batchMetadataFilename = "dropzone/DARTS/submission/DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895.a360";
        // when
        BatchMetadataFilenameProcessor batchMetadataFilenameProcessor = new BatchMetadataFilenameProcessor(batchMetadataFilename);
        // then
        assertEquals("DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895.a360", batchMetadataFilenameProcessor.getBatchMetadataFilename());
        assertEquals("DARTS", batchMetadataFilenameProcessor.getPrefix());
        assertEquals("a17b9015-e6ad-77c5-8d1e-13259aae1895", batchMetadataFilenameProcessor.getUuidString());
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithInvalidFileExtensionThrowsException() {
        // given
        String batchMetadataFilename = "dropzone/DARTS/submission/DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895";
        // when then
        assertThrows(IllegalArgumentException.class, () ->
            new BatchMetadataFilenameProcessor(batchMetadataFilename));
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithInvalidFilenameThrowsException() {
        // given
        String batchMetadataFilename = "dropzone/DARTS/submission/DARTS-a17b9015-e6ad-77c5-8d1e-13259aae1895.a360";
        // when then
        assertThrows(IllegalArgumentException.class, () ->
            new BatchMetadataFilenameProcessor(batchMetadataFilename));
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithNoPath() {
        // given
        String batchMetadataFilename = "DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895.a360";
        // when
        BatchMetadataFilenameProcessor batchMetadataFilenameProcessor = new BatchMetadataFilenameProcessor(batchMetadataFilename);
        // then
        assertEquals("DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895.a360", batchMetadataFilenameProcessor.getBatchMetadataFilename());
        assertEquals("DARTS", batchMetadataFilenameProcessor.getPrefix());
        assertEquals("a17b9015-e6ad-77c5-8d1e-13259aae1895", batchMetadataFilenameProcessor.getUuidString());
    }
}