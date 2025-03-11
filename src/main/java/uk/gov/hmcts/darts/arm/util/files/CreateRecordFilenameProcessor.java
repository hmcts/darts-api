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
public class CreateRecordFilenameProcessor {
    private static final int NUMBER_OF_TOKENS = 4;
    private final String createRecordFilenameAndPath;
    private String hashcode;
    private String hashcode2;
    private String status;
    private String createRecordFilename;

    public CreateRecordFilenameProcessor(String createRecordFilenameAndPath) {
        this.createRecordFilenameAndPath = createRecordFilenameAndPath;
        processFilename();
    }

    private void processFilename() {
        createRecordFilename = FilenameUtils.getName(createRecordFilenameAndPath);
        if (nonNull(createRecordFilename)) {
            List<String> tokens = Arrays.asList(createRecordFilename.split(ARM_FILENAME_SEPARATOR));
            // CR - Create Record - This is the create record file which represents record creation in ARM.
            // DARTS/response/6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp
            if (tokens.size() == NUMBER_OF_TOKENS) {
                hashcode = tokens.getFirst();
                hashcode2 = tokens.get(1);
                status = tokens.get(2);
            } else {
                log.error("Expected {} tokens in create record filename {} but found {}", NUMBER_OF_TOKENS, createRecordFilenameAndPath, tokens.size());
                throw new IllegalArgumentException("Invalid filename " + createRecordFilenameAndPath);
            }
        } else {
            log.error("Invalid create record filename {}", createRecordFilenameAndPath);
            throw new IllegalArgumentException("Invalid create record filename " + createRecordFilenameAndPath);
        }
    }
}
