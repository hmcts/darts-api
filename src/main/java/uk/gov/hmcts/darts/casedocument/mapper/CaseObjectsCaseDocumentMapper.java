package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.casedocument.template.ExternalObjectDirectoryCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.MediaCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.TranscriptionCaseDocument;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BasicMapper.class, ExternalObjectDirectoryRepository.class})
public abstract class CaseObjectsCaseDocumentMapper {

    @Autowired
    ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mappings({
        @Mapping(expression = "java(mapEods(externalObjectDirectoryRepository.findByMedia(mediaEntity)))", target = "externalObjectDirectories"),
    })
    public abstract MediaCaseDocument mapMedia(MediaEntity mediaEntity);

    @Mappings({
        @Mapping(source = "transcriptionDocumentEntities", target = "transcriptionDocuments")
    })
    public abstract TranscriptionCaseDocument map(TranscriptionEntity transcriptionEntity);

    @Mappings({
        @Mapping(expression = "java(mapEods(externalObjectDirectoryRepository.findByTranscriptionDocumentEntity(transcriptionDocumentEntity)))", target = "externalObjectDirectories"),
    })
    public abstract TranscriptionCaseDocument.TranscriptionDocumentCaseDocument mapTranscriptionDocument(TranscriptionDocumentEntity transcriptionDocumentEntity);

    public abstract List<ExternalObjectDirectoryCaseDocument> mapEods(List<ExternalObjectDirectoryEntity> entities);
}
