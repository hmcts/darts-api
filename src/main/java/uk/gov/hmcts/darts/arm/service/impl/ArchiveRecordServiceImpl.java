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
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRecordServiceImpl implements ArchiveRecordService {

    public static final String FILENAME_SEPERATOR = "_";
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private final TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;
    private final AnnotationArchiveRecordMapper annotationArchiveRecordMapper;

    @Transactional
    public Map<String, ArchiveRecordFileInfo> generateArchiveRecord(Integer externalObjectDirectoryId, Integer archiveRecordAttempt) {
        Map<String, ArchiveRecordFileInfo> archiveRecordFileInfoList = new HashMap<>();
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = externalObjectDirectoryRepository.getReferenceById(externalObjectDirectoryId);

            if (nonNull(externalObjectDirectory.getMedia())) {
                generateMediaArchiveRecordFile(externalObjectDirectoryId, archiveRecordAttempt, archiveRecordFileInfoList, externalObjectDirectory);
            } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
                generateTranscriptionArchiveRecordFile(externalObjectDirectoryId, archiveRecordAttempt, archiveRecordFileInfoList, externalObjectDirectory);
            } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
                generateAnnotationArchiveRecordFile(externalObjectDirectoryId, archiveRecordAttempt, archiveRecordFileInfoList, externalObjectDirectory);
            }
        } catch (EntityNotFoundException e) {
            throw new DartsApiException(ArchiveRecordApiError.FAILED_TO_GENERATE_ARCHIVE_RECORD);
        }
        return archiveRecordFileInfoList;
    }

    private void generateAnnotationArchiveRecordFile(Integer externalObjectDirectoryId, Integer archiveRecordAttempt,
                                                     Map<String, ArchiveRecordFileInfo> archiveRecordFileInfoList,
                                                     ExternalObjectDirectoryEntity externalObjectDirectory) {
        String fullFilename = generateArchiveFilename(externalObjectDirectoryId,
                                                      externalObjectDirectory.getAnnotationDocumentEntity().getId(), archiveRecordAttempt
        );
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        AnnotationArchiveRecord annotationArchiveRecord =
            annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, archiveRecordFile);
        boolean generatedArchiveRecord = archiveRecordFileGenerator.generateArchiveRecord(annotationArchiveRecord, archiveRecordFile,
                                                                                          ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE);
        archiveRecordFileInfoList.put(
            ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE.getArchiveTypeDescription(),
            generateArchiveRecordFileInfo(archiveRecordFile, generatedArchiveRecord)
        );
    }

    private void generateTranscriptionArchiveRecordFile(Integer externalObjectDirectoryId, Integer archiveRecordAttempt,
                                                        Map<String, ArchiveRecordFileInfo> archiveRecordFileInfoList,
                                                        ExternalObjectDirectoryEntity externalObjectDirectory) {
        String fullFilename = generateArchiveFilename(externalObjectDirectoryId,
                                                      externalObjectDirectory.getTranscriptionDocumentEntity().getId(), archiveRecordAttempt
        );
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        TranscriptionArchiveRecord transcriptionArchiveRecord =
            transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, archiveRecordFile);
        boolean generatedArchiveRecord = archiveRecordFileGenerator.generateArchiveRecord(transcriptionArchiveRecord, archiveRecordFile,
                                                                                          ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE
        );
        archiveRecordFileInfoList.put(
            ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE.getArchiveTypeDescription(),
            generateArchiveRecordFileInfo(archiveRecordFile, generatedArchiveRecord)
        );
    }

    private void generateMediaArchiveRecordFile(Integer externalObjectDirectoryId, Integer archiveRecordAttempt,
                                                Map<String, ArchiveRecordFileInfo> archiveRecordFileInfoList,
                                                ExternalObjectDirectoryEntity externalObjectDirectory) {
        String fullFilename = generateArchiveFilename(externalObjectDirectoryId,
                                                      externalObjectDirectory.getMedia().getId(), archiveRecordAttempt
        );
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        MediaArchiveRecord mediaArchiveRecord =
            mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, archiveRecordFile);
        boolean generatedArchiveRecord = archiveRecordFileGenerator.generateArchiveRecord(mediaArchiveRecord, archiveRecordFile,
                                                                                          ArchiveRecordType.MEDIA_ARCHIVE_TYPE
        );
        archiveRecordFileInfoList.put(
            ArchiveRecordType.MEDIA_ARCHIVE_TYPE.getArchiveTypeDescription(),
            generateArchiveRecordFileInfo(archiveRecordFile, generatedArchiveRecord)
        );
    }

    private ArchiveRecordFileInfo generateArchiveRecordFileInfo(File archiveRecordFile, boolean generatedArchiveRecord) {
        return ArchiveRecordFileInfo.builder()
            .archiveRecordFile(archiveRecordFile)
            .fileGenerationSuccessful(generatedArchiveRecord)
            .build();
    }

    private String generateArchiveFilename(Integer externalObjectDirectoryId, Integer id, Integer archiveRecordAttempt) {
        return new StringBuilder(externalObjectDirectoryId.toString())
            .append(FILENAME_SEPERATOR)
            .append(id.toString())
            .append(FILENAME_SEPERATOR)
            .append(archiveRecordAttempt)
            .append(armDataManagementConfiguration.getFileExtension())
            .toString();
    }

}
