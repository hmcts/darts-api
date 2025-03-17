package uk.gov.hmcts.darts.arm.util.files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;

@Getter
@Slf4j
public class InvalidLineFileFilenameProcessor {
    private static final int NUMBER_OF_TOKENS = 4;
    private final String invalidLineFileFilenameAndPath;
    private String hashcode;
    private String hashcode2;
    private String status;
    private String invalidLineFilename;

    public InvalidLineFileFilenameProcessor(String invalidLineFileFilenameAndPath) {
        this.invalidLineFileFilenameAndPath = invalidLineFileFilenameAndPath;
        processFilename();
    }

    private void processFilename() {
        invalidLineFilename = FilenameUtils.getName(invalidLineFileFilenameAndPath);
        if (nonNull(invalidLineFilename)) {
            List<String> tokens = Arrays.asList(invalidLineFilename.split(ARM_FILENAME_SEPARATOR));
            // IL - Invalid Lines File - This is the Invalid Lines file which represents the File which is ingested by ARM.
            // DARTS/response/fbfec54925d62146aeced724ff9f3c8e_e5afb388-3830-79ca-a5d4-dcc6e51796a3_0_il.rsp
            if (tokens.size() == NUMBER_OF_TOKENS) {
                hashcode = tokens.getFirst();
                hashcode2 = tokens.get(1);
                status = tokens.get(2);
            } else {
                log.error("Expected {} tokens in filename {} but found {}", NUMBER_OF_TOKENS, invalidLineFileFilenameAndPath, tokens.size());
                throw new IllegalArgumentException("Invalid filename " + invalidLineFileFilenameAndPath);
            }
        } else {
            log.error("Invalid invalid line filename {}", invalidLineFileFilenameAndPath);
            throw new IllegalArgumentException("Invalid invalid line filename " + invalidLineFileFilenameAndPath);
        }
    }
}
