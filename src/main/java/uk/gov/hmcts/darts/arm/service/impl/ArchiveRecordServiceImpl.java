package uk.gov.hmcts.darts.arm.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.exception.ArchiveRecordApiError;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.File;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRecordServiceImpl implements ArchiveRecordService {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private final TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;
    private final AnnotationArchiveRecordMapper annotationArchiveRecordMapper;

    public File generateArchiveRecord(Integer externalObjectDirectoryId, String relationId, String archiveRecordFilename) {
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), archiveRecordFilename);
        boolean generatedArchiveRecord = false;
        ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntityById(externalObjectDirectoryId);
        if (nonNull(externalObjectDirectory.getMedia())) {
            MediaArchiveRecord mediaArchiveRecord =
                mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, relationId, archiveRecordFile);
            generatedArchiveRecord = archiveRecordFileGenerator.generateArchiveRecord(mediaArchiveRecord, archiveRecordFile,
                                                                                      ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
        }
        if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            TranscriptionArchiveRecord transcriptionArchiveRecord =
                transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, relationId, archiveRecordFile);
            generatedArchiveRecord = archiveRecordFileGenerator.generateArchiveRecord(transcriptionArchiveRecord, archiveRecordFile,
                                                                                      ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
        }
        if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            AnnotationArchiveRecord annotationArchiveRecord =
                annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, relationId, archiveRecordFile);
            generatedArchiveRecord = archiveRecordFileGenerator.generateArchiveRecord(annotationArchiveRecord, archiveRecordFile,
                                                                                      ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
        }
        return archiveRecordFile;
    }

    @Transactional()
    ExternalObjectDirectoryEntity getExternalObjectDirectoryEntityById(Integer externalObjectDirectoryId) {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = null;
        try {
            externalObjectDirectoryEntity = externalObjectDirectoryRepository.getReferenceById(externalObjectDirectoryId);
        } catch (EntityNotFoundException e) {
            throw new DartsApiException(ArchiveRecordApiError.FAILED_TO_GENERATE_ARCHIVE_RECORD);
        }
        return externalObjectDirectoryEntity;
    }
}
