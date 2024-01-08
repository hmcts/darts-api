package uk.gov.hmcts.darts.arm.util.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputUploadFilenameProcessorTest {

    @Test
    void givenInputUploadFilenameProcessorValidateFilename() {
        String inputUploadFilename = "12345_111_2_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        InputUploadFilenameProcessor inputUploadFilenameProcessor = new InputUploadFilenameProcessor(inputUploadFilename);

        assertEquals("12345", inputUploadFilenameProcessor.getExternalDirectoryObjectId());
        assertEquals("111", inputUploadFilenameProcessor.getObjectTypeId());
        assertEquals("2", inputUploadFilenameProcessor.getAttempts());
        assertEquals("6a374f19a9ce7dc9cc480ea8d4eca0fb", inputUploadFilenameProcessor.getHashcode());
        assertEquals("1", inputUploadFilenameProcessor.getStatus());
    }

    @Test
    void givenInputUploadFilenameProcessorWithInvalidFileExtensionThrowsException() {
        String inputUploadFilenameProcessor = "12345_111_2_6a374f19a9ce7dc9cc480ea8d4eca0fb_1";

        assertThrows(IllegalArgumentException.class, () ->
            new InputUploadFilenameProcessor(inputUploadFilenameProcessor));
    }

    @Test
    void givenInputUploadFilenameProcessorWithInvalidFilenameThrowsException() {
        String inputUploadFilenameProcessor = "12345111_2_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";

        assertThrows(IllegalArgumentException.class, () ->
            new InputUploadFilenameProcessor(inputUploadFilenameProcessor));
    }
}
