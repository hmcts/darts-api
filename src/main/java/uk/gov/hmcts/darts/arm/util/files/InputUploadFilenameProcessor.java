package uk.gov.hmcts.darts.arm.util.files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl.ARM_FILENAME_SEPARATOR;

@Getter
@Slf4j
public class InputUploadFilenameProcessor {
    private static final int NUMBER_OF_TOKENS = 6;
    private String inputUploadFilename;
    private String externalDirectoryObjectId;
    private String objectTypeId;
    private String attempts;
    private String hashcode;
    private String status;


    public InputUploadFilenameProcessor(String inputUploadFilename) {
        this.inputUploadFilename = inputUploadFilename;
        processFilename();
    }

    private void processFilename() {
        List<String> tokens = Arrays.asList(inputUploadFilename.split(ARM_FILENAME_SEPARATOR));
        // EODID_MEDID_ATTEMPTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp
        if (tokens.size() == NUMBER_OF_TOKENS) {
            externalDirectoryObjectId = tokens.get(0);
            objectTypeId = tokens.get(1);
            attempts = tokens.get(2);
            hashcode = tokens.get(3);
            status = tokens.get(4);
        } else {
            log.error("Expected {} tokens in filename {} but found {}", NUMBER_OF_TOKENS, inputUploadFilename, tokens.size());
            throw new IllegalArgumentException();
        }

    }
}
