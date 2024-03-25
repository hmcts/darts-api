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
public class BatchMetadataFilenameProcessor {

    private static final int NUMBER_OF_TOKENS = 2;
    public static final String FILE_EXTENSION_SEPARATOR = ".";
    private String batchMetadataFilenameAndPath;
    private String batchMetadataFilename;
    private String prefix;
    private String uuidString;

    public BatchMetadataFilenameProcessor(String batchMetadataFilenameAndPath) {
        this.batchMetadataFilenameAndPath = batchMetadataFilenameAndPath;
        processFilename();
    }

    private void processFilename() {
        //Expected filename dropzone/DARTS/submission/DARTS_UUID.a360
        batchMetadataFilename = FilenameUtils.getName(batchMetadataFilenameAndPath);
        if (nonNull(batchMetadataFilename) && batchMetadataFilename.contains(FILE_EXTENSION_SEPARATOR)) {
            String filename = batchMetadataFilename.substring(0, batchMetadataFilename.indexOf(FILE_EXTENSION_SEPARATOR));
            List<String> tokens = Arrays.asList(filename.split(ARM_FILENAME_SEPARATOR));

            if (tokens.size() == NUMBER_OF_TOKENS) {
                prefix = tokens.get(0);
                uuidString = tokens.get(1);
            } else {
                log.error("Expected {} tokens in filename {} but found {}", NUMBER_OF_TOKENS, batchMetadataFilenameAndPath, tokens.size());
                throw new IllegalArgumentException("Invalid filename " + batchMetadataFilenameAndPath);
            }
        } else {
            log.error("Batch ARM pull expected filename to contain extension ", batchMetadataFilenameAndPath);
            throw new IllegalArgumentException("Invalid filename " + batchMetadataFilenameAndPath);
        }
    }
}
