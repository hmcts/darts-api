package uk.gov.hmcts.darts.arm.util.files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;

@Getter
@Slf4j
public class UploadFileFilenameProcessor {

    private static final int NUMBER_OF_TOKENS = 4;
    private final String uploadFileFilename;
    private String hashcode;
    private String hashcode2;
    private String status;

    public UploadFileFilenameProcessor(String uploadFileFilename) {
        this.uploadFileFilename = uploadFileFilename;
        processFilename();
    }

    private void processFilename() {
        List<String> tokens = Arrays.asList(uploadFileFilename.split(ARM_FILENAME_SEPARATOR));
        // UF - Upload File - This is the Upload file which represents the File which is ingested by ARM.
        // 6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp
        if (tokens.size() == NUMBER_OF_TOKENS) {
            hashcode = tokens.get(0);
            hashcode2 = tokens.get(1);
            status = tokens.get(2);
        } else {
            log.error("Expected {} tokens in filename {} but found {}", NUMBER_OF_TOKENS, uploadFileFilename, tokens.size());
            throw new IllegalArgumentException("Invalid filename " + uploadFileFilename);
        }
    }
}
