package uk.gov.hmcts.darts.arm.util.files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;

@Getter
@Slf4j
public class BatchManifestFileFilenameProcessor {

    private static final int NUMBER_OF_TOKENS = 2;
    private static final String FILE_EXTENSION_SEPARATOR = ".";
    private final String batchManifestFilename;
    private String prefix;
    private String uuidString;

    public BatchManifestFileFilenameProcessor(String batchManifestFilename) {
        this.batchManifestFilename = batchManifestFilename;
        processFilename();
    }

    private void processFilename() {
        //Expected filename DARTS_56f52825-745f-4b33-b51a-92af826b007d.a360
        if (nonNull(batchManifestFilename) && batchManifestFilename.contains(FILE_EXTENSION_SEPARATOR)) {
            String filename = StringUtils.substringBefore(batchManifestFilename, FILE_EXTENSION_SEPARATOR);
            List<String> tokens = Arrays.asList(filename.split(ARM_FILENAME_SEPARATOR));

            if (tokens.size() == NUMBER_OF_TOKENS) {
                prefix = tokens.get(0);
                uuidString = tokens.get(1);
            } else {
                log.error("Expected {} tokens in batch manifest filename {} but found {}", NUMBER_OF_TOKENS, batchManifestFilename, tokens.size());
                throw new IllegalArgumentException("Invalid batch input upload filename " + batchManifestFilename);
            }
        } else {
            log.error("Batch ARM pull expected manifest filename to contain extension ", batchManifestFilename);
            throw new IllegalArgumentException("Invalid batch input upload filename " + batchManifestFilename);
        }
    }
}
