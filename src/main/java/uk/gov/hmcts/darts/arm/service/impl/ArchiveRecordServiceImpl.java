package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRecordServiceImpl implements ArchiveRecordService {

    public static final String FILENAME_SEPARATOR = "_";
    public static final String FILE_EXTENSION_PERIOD = ".";

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private final TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;
    private final AnnotationArchiveRecordMapper annotationArchiveRecordMapper;
    private final CaseArchiveRecordMapper caseArchiveRecordMapper;

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;


    @Transactional
    public ArchiveRecordFileInfo generateArchiveRecord(Integer externalObjectDirectoryId, String rawFilename) {
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(false)
            .build();

        try {
            // Eager load ExternalObjectDirectoryEntity
            Optional<ExternalObjectDirectoryEntity> externalObjectDirectoryOptional =
                externalObjectDirectoryRepository.findById(externalObjectDirectoryId);

            if (externalObjectDirectoryOptional.isPresent()) {
                ExternalObjectDirectoryEntity externalObjectDirectory = externalObjectDirectoryOptional.get();
                Integer archiveRecordAttempt = externalObjectDirectory.getTransferAttempts();

                if (nonNull(externalObjectDirectory.getMedia())) {
                    generateMediaArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo, rawFilename);
                } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
                    generateTranscriptionArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo, rawFilename);
                } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
                    generateAnnotationArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo, rawFilename);
                } else if (nonNull((externalObjectDirectory.getCaseDocument()))) {
                    generateCaseArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo, rawFilename);
                }
            }
        } catch (IOException e) {
            log.error("Unable to generate archive record {}", e.getMessage());
        }
        return archiveRecordFileInfo;
    }

    private void generateCaseArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                               ArchiveRecordFileInfo archiveRecordFileInfo, String rawFilename) throws IOException {
        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.CASE_ARCHIVE_TYPE);

        String fullFilename = generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getCaseDocument().getId(), archiveRecordAttempt);

        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        CaseArchiveRecord caseArchiveRecord =
            caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, rawFilename);

        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(caseArchiveRecord, archiveRecordFile, ArchiveRecordType.CASE_ARCHIVE_TYPE)
        );
    }


    private void generateMediaArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                ArchiveRecordFileInfo archiveRecordFileInfo, String rawFilename) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.MEDIA_ARCHIVE_TYPE);

        String fullFilename = generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getMedia().getId(), archiveRecordAttempt);

        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        MediaArchiveRecord mediaArchiveRecord = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, rawFilename);
        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(mediaArchiveRecord, archiveRecordFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE)
        );

    }

    private void generateTranscriptionArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                        ArchiveRecordFileInfo archiveRecordFileInfo, String rawFilename) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE);

        String fullFilename =
            generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getTranscriptionDocumentEntity().getId(),
                                    archiveRecordAttempt);
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        TranscriptionArchiveRecord transcriptionArchiveRecord =
            transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, rawFilename);

        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(transcriptionArchiveRecord, archiveRecordFile, ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE)
        );
    }

    private void generateAnnotationArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                     ArchiveRecordFileInfo archiveRecordFileInfo, String rawFilename) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE);

        String fullFilename =
            generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getAnnotationDocumentEntity().getId(), archiveRecordAttempt);
        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        AnnotationArchiveRecord annotationArchiveRecord =
            annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, rawFilename);

        archiveRecordFileInfo.setFileGenerationSuccessful(
            archiveRecordFileGenerator.generateArchiveRecord(annotationArchiveRecord, archiveRecordFile, ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE)
        );
    }


    private String generateArchiveFilename(Integer externalObjectDirectoryId, Integer id, Integer archiveRecordAttempt) {
        return new StringBuilder(externalObjectDirectoryId.toString())
            .append(FILENAME_SEPARATOR)
            .append(id.toString())
            .append(FILENAME_SEPARATOR)
            .append(archiveRecordAttempt)
            .append(FILE_EXTENSION_PERIOD)
            .append(armDataManagementConfiguration.getFileExtension())
            .toString();
    }

    @Transactional
    public ArchiveRecord generateArchiveRecordInfo(Integer externalObjectDirectoryId, String rawFilename) {

        ExternalObjectDirectoryEntity externalObjectDirectory = externalObjectDirectoryRepository.findById(externalObjectDirectoryId).orElseThrow(
            () -> new DartsException(format("external object directory not found with id: %d", externalObjectDirectoryId)));

        ArchiveRecord result;

        if (nonNull(externalObjectDirectory.getMedia())) {
            result = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, rawFilename);
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            result = transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, rawFilename);
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            result = annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, rawFilename);
        } else if (nonNull((externalObjectDirectory.getCaseDocument()))) {
            result = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, rawFilename);
        } else {
            throw new DartsException(String.format("unknown archive record type for EOD %d", externalObjectDirectoryId));
        }

        if (result == null) {
            throw new DartsException(String.format("exception generating archive record for EOD %d", externalObjectDirectoryId));
        } else {
            return result;
        }
    }

}
