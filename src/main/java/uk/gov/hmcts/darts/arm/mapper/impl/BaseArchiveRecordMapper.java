package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;

import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_001_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_002_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_003_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_004_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_005_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_006_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_007_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_008_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_009_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_010_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_011_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_012_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_013_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_014_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_015_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_016_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_017_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_018_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_019_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_020_KEY;

@Slf4j
public class BaseArchiveRecordMapper {

    protected void processStringMetadataProperties(RecordMetadata metadata, String key, String value) {
        switch (key) {
            case BF_001_KEY -> metadata.setBf001(value);
            case BF_002_KEY -> metadata.setBf002(value);
            case BF_003_KEY -> metadata.setBf003(value);
            case BF_004_KEY -> metadata.setBf004(value);
            case BF_005_KEY -> metadata.setBf005(value);
            case BF_006_KEY -> metadata.setBf006(value);
            case BF_007_KEY -> metadata.setBf007(value);
            case BF_008_KEY -> metadata.setBf008(value);
            case BF_009_KEY -> metadata.setBf009(value);
            case BF_010_KEY -> metadata.setBf010(value);
            case BF_011_KEY -> metadata.setBf011(value);
            case BF_016_KEY -> metadata.setBf016(value);
            case BF_017_KEY -> metadata.setBf017(value);
            case BF_018_KEY -> metadata.setBf018(value);
            case BF_019_KEY -> metadata.setBf019(value);
            case BF_020_KEY -> metadata.setBf020(value);
            default -> log.warn("Archive record unknown property key: {}", key);
        }
    }

    protected void processIntMetadataProperties(RecordMetadata metadata, String key, Integer intValue) {
        if (intValue != null) {
            switch (key) {
                case BF_012_KEY -> metadata.setBf012(intValue);
                case BF_013_KEY -> metadata.setBf013(intValue);
                case BF_014_KEY -> metadata.setBf014(intValue);
                case BF_015_KEY -> metadata.setBf015(intValue);
                default -> log.warn("Archive record unknown integer property key: {}", key);
            }
        }
    }


}
