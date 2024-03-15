package uk.gov.hmcts.darts.common.datamanagement.api;

import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.util.List;

/**
 * An interface that mediates between the three data management APIs.
 * {@link uk.gov.hmcts.darts.datamanagement.api.DataManagementApi}
 * {@link uk.gov.hmcts.darts.dets.api.DetsDataManagementApi}
 * {@link uk.gov.hmcts.darts.arm.api.ArmDataManagementApi}
 */
public interface DataManagementFacade {

    DownloadResponseMetaData retrieveFileFromStorage(MediaEntity mediaEntity) throws FileNotDownloadedException;

    DownloadResponseMetaData retrieveFileFromStorage(TranscriptionDocumentEntity transcriptionDocumentEntity) throws FileNotDownloadedException;

    DownloadResponseMetaData retrieveFileFromStorage(AnnotationDocumentEntity annotationDocumentEntity) throws FileNotDownloadedException;

    DownloadResponseMetaData retrieveFileFromStorage(List<ExternalObjectDirectoryEntity> eodEntities) throws FileNotDownloadedException;
}