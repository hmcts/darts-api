package uk.gov.hmcts.darts.arm.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.arm.component.impl.ArchiveRecordFileGeneratorImpl;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.api.Assertions.assertFalse;


class ArchiveRecordFileGeneratorIntTest extends IntegrationBase {
    private ArchiveRecordFileGeneratorImpl archiveRecordFileGenerator;

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();
        archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(objectMapper);

    }

    @Test
    void generateArchiveRecordWithAllNullParameters() {
        assertFalse(archiveRecordFileGenerator.generateArchiveRecord(null, null, null));

    }

    @Test
    void generateArchiveRecordWithNullArchiveRecordAndNullFilename() {
        assertFalse(archiveRecordFileGenerator.generateArchiveRecord(null, null, ArchiveRecordType.MEDIA_ARCHIVE_TYPE));
    }
}
