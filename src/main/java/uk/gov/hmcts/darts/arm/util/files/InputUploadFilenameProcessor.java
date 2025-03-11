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
public class InputUploadFilenameProcessor {
    private static final int NUMBER_OF_TOKENS = 6;
    private final String inputUploadFilenameAndPath;
    private String externalDirectoryObjectId;
    private String objectTypeId;
    private String attempts;
    private String hashcode;
    private String status;
    private String inputUploadFilename;


    public InputUploadFilenameProcessor(String inputUploadFilenameAndPath) {
        this.inputUploadFilenameAndPath = inputUploadFilenameAndPath;
        processFilename();
    }

    private void processFilename() {
        inputUploadFilename = FilenameUtils.getName(inputUploadFilenameAndPath);
        if (nonNull(inputUploadFilename)) {
            List<String> tokens = Arrays.asList(inputUploadFilename.split(ARM_FILENAME_SEPARATOR));
            // DARTS/response/EODID_MEDID_ATTEMPTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp
            if (tokens.size() == NUMBER_OF_TOKENS) {
                externalDirectoryObjectId = tokens.getFirst();
                objectTypeId = tokens.get(1);
                attempts = tokens.get(2);
                hashcode = tokens.get(3);
                status = tokens.get(4);
            } else {
                log.error("Expected {} tokens in input upload filename {} but found {}", NUMBER_OF_TOKENS, inputUploadFilenameAndPath, tokens.size());
                throw new IllegalArgumentException("Invalid filename " + inputUploadFilenameAndPath);
            }
        } else {
            log.error("Invalid input upload filename {}", inputUploadFilenameAndPath);
            throw new IllegalArgumentException("Invalid input upload filename " + inputUploadFilenameAndPath);
        }
    }
}
