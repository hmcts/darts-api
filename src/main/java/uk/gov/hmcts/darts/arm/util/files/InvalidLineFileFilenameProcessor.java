package uk.gov.hmcts.darts.arm.util.files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;

@Getter
@Slf4j
public class InvalidLineFileFilenameProcessor {
    private static final int NUMBER_OF_TOKENS = 4;
    private String invalidLineFileFilename;
    private String hashcode;
    private String hashcode2;
    private String status;

    public InvalidLineFileFilenameProcessor(String invalidLineFileFilename) {
        this.invalidLineFileFilename = invalidLineFileFilename;
        processFilename();
    }

    private void processFilename() {
        List<String> tokens = Arrays.asList(invalidLineFileFilename.split(ARM_FILENAME_SEPARATOR));
        // IL - Invalid Lines File - This is the Invalid Lines file which represents the File which is ingested by ARM.
        // fbfec54925d62146aeced724ff9f3c8e_e5afb388-3830-79ca-a5d4-dcc6e51796a3_0_il.rsp
        if (tokens.size() == NUMBER_OF_TOKENS) {
            hashcode = tokens.get(0);
            hashcode2 = tokens.get(1);
            status = tokens.get(2);
        } else {
            log.error("Expected {} tokens in filename {} but found {}", NUMBER_OF_TOKENS, invalidLineFileFilename, tokens.size());
            throw new IllegalArgumentException("Invalid filename " + invalidLineFileFilename);
        }
    }
}
