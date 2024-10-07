package uk.gov.hmcts.darts.arm.helper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.NEW;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class DataStoreToArmHelperIntTest extends IntegrationBase {

    @Autowired
    private DataStoreToArmHelper dataStoreToArmHelper;

    @Autowired
    private EodHelper eodHelper;
    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;


    @Test
    void ignoreArmDropZoneStatus() {
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_DROP_ZONE, ARM);

        List<ExternalObjectDirectoryEntity> eodEntitiesToSendToArm = dataStoreToArmHelper.getEodEntitiesToSendToArm(EodHelper.unstructuredLocation(),
                                                                                                                    EodHelper.armLocation(), 5);
        assertEquals(1, eodEntitiesToSendToArm.size());

    }

    @Test
    void getCorrectEodEntities() {
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), NEW, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), STORED, UNSTRUCTURED);
        ExternalObjectDirectoryEntity failedTooManyTimesEod = externalObjectDirectoryStub.createAndSaveEod(medias.get(4), ARM_MANIFEST_FAILED, ARM);
        failedTooManyTimesEod.setTransferAttempts(4);
        dartsDatabase.save(failedTooManyTimesEod);

        List<ExternalObjectDirectoryEntity> eodEntitiesToSendToArm = dataStoreToArmHelper.getEodEntitiesToSendToArm(EodHelper.unstructuredLocation(),
                                                                                                                    EodHelper.armLocation(), 5);
        assertEquals(3, eodEntitiesToSendToArm.size());

    }
}
