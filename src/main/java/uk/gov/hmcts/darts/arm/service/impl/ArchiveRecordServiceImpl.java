package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRecordServiceImpl implements ArchiveRecordService {

    public static final String FILENAME_SEPERATOR = "_";
    public static final String FILE_EXTENSION_PERIOD = ".";

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private final TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;
    private final AnnotationArchiveRecordMapper annotationArchiveRecordMapper;

    public ArchiveRecordFileInfo generateArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                       Integer archiveRecordAttempt) {
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(false)
            .build();
        try {
            if (nonNull(externalObjectDirectory.getMedia())) {
                generateMediaArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);
            } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
                generateTranscriptionArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);
            } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
                generateAnnotationArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);
            }
        } catch (IOException e) {
            log.error("Unable to generate archive record {}", e.getMessage());
        }
        return archiveRecordFileInfo;
    }


    private void generateMediaArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                ArchiveRecordFileInfo archiveRecordFileInfo) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.MEDIA_ARCHIVE_TYPE);

        String fullFilename = generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getMedia().getId(), archiveRecordAttempt);

        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        MediaArchiveRecord mediaArchiveRecord = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, archiveRecordFile);
        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(mediaArchiveRecord, archiveRecordFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE)
        );

    }

    private void generateTranscriptionArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                        ArchiveRecordFileInfo archiveRecordFileInfo) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE);

        String fullFilename =
            generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getTranscriptionDocumentEntity().getId(), archiveRecordAttempt);
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        TranscriptionArchiveRecord transcriptionArchiveRecord =
            transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, archiveRecordFile);

        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(transcriptionArchiveRecord, archiveRecordFile, ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE)
        );
    }

    private void generateAnnotationArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                     ArchiveRecordFileInfo archiveRecordFileInfo) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE);

        String fullFilename =
            generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getAnnotationDocumentEntity().getId(), archiveRecordAttempt);
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        AnnotationArchiveRecord annotationArchiveRecord =
            annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, archiveRecordFile);

        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(annotationArchiveRecord, archiveRecordFile, ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE)
        );
    }

    private String generateArchiveFilename(Integer externalObjectDirectoryId, Integer id, Integer archiveRecordAttempt) {
        return new StringBuilder(externalObjectDirectoryId.toString())
            .append(FILENAME_SEPERATOR)
            .append(id.toString())
            .append(FILENAME_SEPERATOR)
            .append(archiveRecordAttempt)
            .append(FILE_EXTENSION_PERIOD)
            .append(armDataManagementConfiguration.getFileExtension())
            .toString();
    }

}
