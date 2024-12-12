package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRecordServiceImpl implements ArchiveRecordService {

    private final MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private final TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;
    private final AnnotationArchiveRecordMapper annotationArchiveRecordMapper;
    private final CaseArchiveRecordMapper caseArchiveRecordMapper;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Transactional
    @Override
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
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            result = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, rawFilename);
        } else {
            throw new DartsException(format("unknown archive record type for EOD %d", externalObjectDirectoryId));
        }

        if (result == null) {
            throw new DartsException(format("exception generating archive record for EOD %d", externalObjectDirectoryId));
        } else {
            return result;
        }
    }

}
