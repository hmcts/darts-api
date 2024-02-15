package uk.gov.hmcts.darts.arm.util.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvalidLineFilenameProcessorTest {

    @Test
    void givenInvalidLineFilenameProcessorValidateFilename() {
        String invalidLineFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor = new InvalidLineFileFilenameProcessor(invalidLineFilename);

        assertEquals("6a374f19a9ce7dc9cc480ea8d4eca0fb", invalidLineFileFilenameProcessor.getHashcode());
        assertEquals("04e6bc3b-952a-79b6-8362-13259aae1895", invalidLineFileFilenameProcessor.getHashcode2());
        assertEquals("0", invalidLineFileFilenameProcessor.getStatus());
    }

    @Test
    void givenInvalidLineFilenameProcessorWithInvalidFileExtensionThrowsException() {
        String invalidLineFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0";

        assertThrows(IllegalArgumentException.class, () ->
            new InvalidLineFileFilenameProcessor(invalidLineFilename));
    }

    @Test
    void givenInvalidLineFilenameProcessorWithInvalidFileNameThrowsException() {
        String invalidLineFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";

        assertThrows(IllegalArgumentException.class, () ->
            new InvalidLineFileFilenameProcessor(invalidLineFilename));
    }
}
