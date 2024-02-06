package uk.gov.hmcts.darts.arm.util.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadFileFilenameProcessorTest {

    @Test
    void givenUploadFileFilenameProcessorValidateFilename() {
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFileFilename);

        assertEquals("6a374f19a9ce7dc9cc480ea8d4eca0fb", uploadFileFilenameProcessor.getHashcode());
        assertEquals("04e6bc3b-952a-79b6-8362-13259aae1895", uploadFileFilenameProcessor.getHashcode2());
        assertEquals("1", uploadFileFilenameProcessor.getStatus());
    }

    @Test
    void givenCreateRecordFilenameProcessorWithInvalidFileExtensionThrowsException() {
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1";

        assertThrows(IllegalArgumentException.class, () ->
              new UploadFileFilenameProcessor(uploadFileFilename));
    }

    @Test
    void givenCreateRecordFilenameProcessorWithInvalidFilenameThrowsException() {
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";

        assertThrows(IllegalArgumentException.class, () ->
              new UploadFileFilenameProcessor(uploadFileFilename));
    }
}
