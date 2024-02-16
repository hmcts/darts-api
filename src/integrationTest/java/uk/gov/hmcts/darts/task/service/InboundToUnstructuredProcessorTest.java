package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.testutils.data.MediaTestData.getBinaryData;

@SuppressWarnings("PMD.ExcessiveImports")
class InboundToUnstructuredProcessorTest extends IntegrationBase {

    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @SpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @MockBean
    DataManagementService dataManagementService;

    @Autowired
    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    @BeforeEach
    public void setup() {
        externalObjectDirectoryStub = dartsDatabase.getExternalObjectDirectoryStub();
    }

    @Test
    void testInboundToUnstructured() {
        // given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        var media1 = medias.get(0);
        var media2 = medias.get(1);
        var media3 = medias.get(2);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(getBinaryData());
        when(dataManagementService.saveBlobData(any(), any())).thenReturn(UUID.randomUUID());

        //matches because no corresponding unstructured
        externalObjectDirectoryStub.createAndSaveEOD(media1, STORED, INBOUND);

        //matches because unstructured failed with no max attempts reached
        externalObjectDirectoryStub.createAndSaveEOD(media2, STORED, INBOUND);
        externalObjectDirectoryStub.createAndSaveEOD(media2, FAILURE, UNSTRUCTURED);

        //does not match because corresponding unstructured is stored
        externalObjectDirectoryStub.createAndSaveEOD(media3, STORED, INBOUND);
        externalObjectDirectoryStub.createAndSaveEOD(media3, STORED, UNSTRUCTURED);

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media1, STORED, UNSTRUCTURED)).hasSize(1);
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media2, STORED, UNSTRUCTURED)).hasSize(1);
        var argument = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(argument.capture());
        List<ExternalObjectDirectoryEntity> createdUnstructured = argument.getAllValues();
        List<Integer> createdUnstructuredMediaIds = createdUnstructured.stream().map(eod -> eod.getMedia().getId()).collect(toList());
        assertThat(createdUnstructuredMediaIds).contains(media1.getId(), media2.getId());
        assertThat(createdUnstructuredMediaIds).doesNotContain(media3.getId());
    }

}
