package uk.gov.hmcts.darts.annotation.service.impl;

import com.azure.core.util.BinaryData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;

@ExtendWith(MockitoExtension.class)
class AnnotationDataManagementTest {

    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private DataManagementFacade dataManagementFacade;
    @Mock
    private DownloadableExternalObjectDirectories downloadableExternalObjectDirectories;
    @Mock
    private DownloadResponseMetaData downloadResponseMetaData;

    private AnnotationDataManagement annotationDataManagement;

    @BeforeEach
    void setUp() {
        annotationDataManagement = new AnnotationDataManagement(dataManagementApi, dataManagementFacade);
    }

    @Test
    void throwsWhenSavingToInboundContainerFails() {
        var binaryData = BinaryData.fromBytes("some-binary-data".getBytes());
        when(dataManagementApi.saveBlobDataToInboundContainer(binaryData)).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> annotationDataManagement.upload(binaryData, "test.pdf"))
              .isInstanceOf(DartsApiException.class)
              .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);

        verify(dataManagementApi, never()).saveBlobDataToUnstructuredContainer(any());
    }

    @Test
    void throwsAndAttemptsToDeleteFromInboundContainerWhenSavingToUnstructuredContainerFails() throws AzureDeleteBlobException {
        var binaryData = BinaryData.fromBytes("some-binary-data".getBytes());
        var inboundLocationUuid = UUID.randomUUID();
        when(dataManagementApi.saveBlobDataToInboundContainer(binaryData)).thenReturn(inboundLocationUuid);
        when(dataManagementApi.saveBlobDataToUnstructuredContainer(binaryData)).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> annotationDataManagement.upload(binaryData, "test.pdf"))
              .isInstanceOf(DartsApiException.class)
              .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);

        verify(dataManagementApi, times(1)).deleteBlobDataFromInboundContainer(inboundLocationUuid);

    }

    @Test
    void returnsContainerLocationsWhenUploadSucceeds() {
        var binaryData = BinaryData.fromBytes("some-binary-data".getBytes());
        var inboundLocationUuid = UUID.randomUUID();
        var unstructuredLocationUuid = UUID.randomUUID();
        when(dataManagementApi.saveBlobDataToInboundContainer(binaryData)).thenReturn(inboundLocationUuid);
        when(dataManagementApi.saveBlobDataToUnstructuredContainer(binaryData)).thenReturn(unstructuredLocationUuid);

        var containerLocations = annotationDataManagement.upload(binaryData, "test.pdf");

        assertThat(containerLocations.get(INBOUND)).isEqualTo(inboundLocationUuid);
        assertThat(containerLocations.get(UNSTRUCTURED)).isEqualTo(unstructuredLocationUuid);
    }

    @Test
    void throwsWhenDeleteFails() throws AzureDeleteBlobException {
        var externalLocationUuid = UUID.randomUUID();
        doThrow(new AzureDeleteBlobException("some-message")).when(dataManagementApi).deleteBlobDataFromInboundContainer(externalLocationUuid);

        assertThatThrownBy(() -> annotationDataManagement.attemptToDeleteDocuments(Map.of(INBOUND, externalLocationUuid)))
              .isInstanceOf(DartsApiException.class)
              .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);
    }

    @Test
    void throwsIfDownloadAnnotationDocumentResponseFails() {
        assertThatThrownBy(() -> annotationDataManagement.download(Arrays.asList(someExternalObjectDirectoryEntity())))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT);
    }

    @SneakyThrows
    @Test
    void throwsIfDownloadAnnotationDocumentInputStreamFails() {
        try (MockedStatic<DownloadableExternalObjectDirectories> mockedStatic = Mockito.mockStatic(DownloadableExternalObjectDirectories.class)) {
            when(DownloadableExternalObjectDirectories.getFileBasedDownload(anyList())).thenReturn(downloadableExternalObjectDirectories);
            when(downloadableExternalObjectDirectories.getResponse()).thenReturn(downloadResponseMetaData);
            when(downloadResponseMetaData.isSuccessfulDownload()).thenReturn(true);
            when(downloadResponseMetaData.getInputStream()).thenThrow(new IOException());

            assertThatThrownBy(() -> annotationDataManagement.download(Arrays.asList(someExternalObjectDirectoryEntity())))
                .isInstanceOf(DartsApiException.class)
                .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT);
        }
    }

    private ExternalObjectDirectoryEntity someExternalObjectDirectoryEntity() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setId(1);
        externalObjectDirectoryEntity.setAnnotationDocumentEntity(new AnnotationDocumentEntity());
        return externalObjectDirectoryEntity;
    }
}
