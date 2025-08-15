package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class UnstructuredToArmAutomatedTaskIntTest extends IntegrationBase {

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask;

    @MockitoBean
    private AzureCopyUtil azureCopyUtil;

    @Test
    void runTask_ShouldThrowInterruptedException() {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();

        externalObjectDirectoryStub.createAndSaveEod(medias.getFirst(), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.getFirst(), ARM_RAW_DATA_FAILED, ARM);

        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(azureCopyUtil).copy(any(), any());

        unstructuredToArmAutomatedTask.preRunTask();

        //when
        unstructuredToArmAutomatedTask.runTask();

        //then
        var foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findMediaIdsByInMediaIdStatusAndType(
            List.of(medias.getFirst().getId()),
            EodHelper.armDropZoneStatus(),
            EodHelper.armLocation()
        );
        assertThat(foundMediaList.size()).isEqualTo(1);
        assertThat(dartsDatabase.getExternalObjectDirectoryRepository().findMediaIdsByInMediaIdStatusAndType(
            List.of(medias.getFirst().getId()), EodHelper.storedStatus(), EodHelper.unstructuredLocation())).hasSize(1);
    }
}
