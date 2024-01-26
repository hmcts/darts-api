package uk.gov.hmcts.darts.arm.util.files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;

@Getter
@Slf4j
public class CreateRecordFilenameProcessor {
    private static final int NUMBER_OF_TOKENS = 4;
    private String createRecordFilename;
    private String hashcode;
    private String hashcode2;
    private String status;

    public CreateRecordFilenameProcessor(String createRecordFilename) {
        this.createRecordFilename = createRecordFilename;
        processFilename();
    }

    private void processFilename() {
        List<String> tokens = Arrays.asList(createRecordFilename.split(ARM_FILENAME_SEPARATOR));
        // CR - Create Record - This is the create record file which represents record creation in ARM.
        // 6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp
        if (tokens.size() == NUMBER_OF_TOKENS) {
            hashcode = tokens.get(0);
            hashcode2 = tokens.get(1);
            status = tokens.get(2);
        } else {
            log.error("Expected {} tokens in filename {} but found {}", NUMBER_OF_TOKENS, createRecordFilename, tokens.size());
            throw new IllegalArgumentException("Invalid filename " + createRecordFilename);
        }
    }
}
