package uk.gov.hmcts.darts.arm.util.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchInputUploadFileFilenameProcessorTest {

    @Test
    void givenBatchMetadataFilenameProcessorValidateFilename() {
        // given DARTS_uuid_hashcode_1_iu.rsp
        String batchMetadataFilename = "dropzone/DARTS/response/DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        // when
        BatchInputUploadFileFilenameProcessor batchMetadataFilenameProcessor = new BatchInputUploadFileFilenameProcessor(batchMetadataFilename);
        // then
        assertEquals("DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp",
                     batchMetadataFilenameProcessor.getBatchMetadataFilename());
        assertEquals("DARTS", batchMetadataFilenameProcessor.getPrefix());
        assertEquals("a17b9015-e6ad-77c5-8d1e-13259aae1895", batchMetadataFilenameProcessor.getUuidString());
        assertEquals("6a374f19a9ce7dc9cc480ea8d4eca0fb", batchMetadataFilenameProcessor.getHashcode());
        assertEquals("1", batchMetadataFilenameProcessor.getStatus());
        assertEquals(batchMetadataFilename, batchMetadataFilenameProcessor.getBatchMetadataFilenameAndPath());
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithInvalidFileExtensionThrowsException() {
        // given
        String batchMetadataFilename = "dropzone/DARTS/response/DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895";
        // when then
        assertThrows(IllegalArgumentException.class, () ->
            new BatchInputUploadFileFilenameProcessor(batchMetadataFilename));
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithInvalidFilenameThrowsException() {
        // given
        String batchMetadataFilename = "dropzone/DARTS/response/DARTS-a17b9015-e6ad-77c5-8d1e-13259aae1895.a360";
        // when then
        assertThrows(IllegalArgumentException.class, () ->
            new BatchInputUploadFileFilenameProcessor(batchMetadataFilename));
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithNullFilenameThrowsException() {
        // given
        String batchMetadataFilename = null;
        // when then
        assertThrows(IllegalArgumentException.class, () ->
            new BatchInputUploadFileFilenameProcessor(batchMetadataFilename));
    }

    @Test
    void givenBatchMetadataFilenameProcessorWithNoPath() {
        // given
        String batchMetadataFilename = "DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        // when
        BatchInputUploadFileFilenameProcessor batchMetadataFilenameProcessor = new BatchInputUploadFileFilenameProcessor(batchMetadataFilename);
        // then
        assertEquals("DARTS_a17b9015-e6ad-77c5-8d1e-13259aae1895_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp",
                     batchMetadataFilenameProcessor.getBatchMetadataFilename());
        assertEquals("DARTS", batchMetadataFilenameProcessor.getPrefix());
        assertEquals("a17b9015-e6ad-77c5-8d1e-13259aae1895", batchMetadataFilenameProcessor.getUuidString());
        assertEquals("6a374f19a9ce7dc9cc480ea8d4eca0fb", batchMetadataFilenameProcessor.getHashcode());
        assertEquals("1", batchMetadataFilenameProcessor.getStatus());
    }
}