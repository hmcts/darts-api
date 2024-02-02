package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.template.AnnotationRecordTemplateMapper;
import uk.gov.hmcts.darts.arm.mapper.template.CaseRecordTemplateMapper;
import uk.gov.hmcts.darts.arm.mapper.template.MediaRecordTemplateMapper;
import uk.gov.hmcts.darts.arm.mapper.template.TranscriptionRecordTemplateMapper;
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
    private final AnnotationRecordTemplateMapper annotationRecordTemplateMapper;
    private final CaseRecordTemplateMapper caseRecordTemplateMapper;
    private final MediaRecordTemplateMapper mediaRecordTemplateMapper;
    private final TranscriptionRecordTemplateMapper transcriptionRecordTemplateMapper;

    @Autowired
    ResourceLoader resourceLoader;


    public ArchiveRecordFileInfo generateArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory) {
        ArchiveRecordFileInfo archiveRecordFileInfo = ArchiveRecordFileInfo.builder()
            .fileGenerationSuccessful(false)
            .build();
        try {
            Integer archiveRecordAttempt = externalObjectDirectory.getTransferAttempts();

            if (nonNull(externalObjectDirectory.getMedia())) {
                generateMediaArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);
            } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
                generateTranscriptionArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);
            } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
                generateAnnotationArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);
            } else if (nonNull((externalObjectDirectory.getCaseDocument()))) {
                generateCaseArchiveRecordFile(externalObjectDirectory, archiveRecordAttempt, archiveRecordFileInfo);

            }
        } catch (IOException e) {
            log.error("Unable to generate archive record {}", e.getMessage());
        }
        return archiveRecordFileInfo;
    }

    private void generateCaseArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                               ArchiveRecordFileInfo archiveRecordFileInfo) throws IOException {
        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.CASE_ARCHIVE_TYPE);

        String fullFilename = generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getMedia().getId(), archiveRecordAttempt);

        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

    }


    private void generateMediaArchiveRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt,
                                                ArchiveRecordFileInfo archiveRecordFileInfo) throws IOException {

        archiveRecordFileInfo.setArchiveRecordType(ArchiveRecordType.MEDIA_ARCHIVE_TYPE);

        String fullFilename = generateArchiveFilename(externalObjectDirectory.getId(), externalObjectDirectory.getMedia().getId(), archiveRecordAttempt);

        File archiveRecordFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fullFilename);
        archiveRecordFileInfo.setArchiveRecordFile(archiveRecordFile);
        Files.createDirectories(archiveRecordFile.getParentFile().toPath());

        String templateFileContents = getContentsFromFile(armDataManagementConfiguration.getMediaRecordTemplate());

        String mappedTemplateFileContents = mediaRecordTemplateMapper.mapTemplateContents(externalObjectDirectory, templateFileContents);
        log.info("Mapped contents {}", mappedTemplateFileContents);

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

    private String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource(filelocation).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }

    private String readTemplateFile(String templateFile) throws IOException {
        File file = new File(getClass().getResource(templateFile).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

    private String readTemplateFileToString(String templateFile) throws IOException {
        //File resource = loadTemplateWithResourceLoader(templateFile).getFile();
        File resource = new ClassPathResource(templateFile).getFile();
        return new String(Files.readAllBytes(resource.toPath()));
    }

    private Resource loadTemplateWithResourceLoader(String templateFile) {
        return resourceLoader.getResource(templateFile);
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
