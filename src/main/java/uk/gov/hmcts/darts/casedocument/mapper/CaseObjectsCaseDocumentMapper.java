package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.casedocument.model.AnnotationCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.ExternalObjectDirectoryCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.MediaCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.TranscriptionCaseDocument;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
    BasicCaseDocumentConversions.class, ExternalObjectDirectoryRepository.class
})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public abstract class CaseObjectsCaseDocumentMapper {

    @Autowired
    ExternalObjectDirectoryRepository eodRepository;

    @Mappings({
        @Mapping(expression = "java(mapEods(eodRepository.findByMedia(mediaEntity)))", target = "externalObjectDirectories"),
        @Mapping(source = "objectAdminActions", target = "adminActionReasons"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById"),
        @Mapping(target = "createdBy", source = "createdById")
    })
    public abstract MediaCaseDocument map(MediaEntity mediaEntity);

    @Mappings({
        @Mapping(source = "transcriptionDocumentEntities", target = "transcriptionDocuments"),
        @Mapping(source = "transcriptionCommentEntities", target = "transcriptionComments"),
        @Mapping(source = "transcriptionWorkflowEntities", target = "transcriptionWorkflows"),
        @Mapping(source = "requestedBy.userFullName", target = "requestor"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById"),
        @Mapping(target = "createdBy", source = "createdById")
    })
    public abstract TranscriptionCaseDocument map(TranscriptionEntity entity);

    @Mappings({
        @Mapping(expression = "java(mapEods(eodRepository.findByTranscriptionDocumentEntity(entity)))", target = "externalObjectDirectories"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    public abstract TranscriptionCaseDocument.TranscriptionDocumentCaseDocument map(TranscriptionDocumentEntity entity);

    @Mappings({
        @Mapping(expression = "java(mapEods(eodRepository.findByAnnotationDocumentEntity(entity)))", target = "externalObjectDirectories"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    public abstract AnnotationCaseDocument.AnnotationDocumentCaseDocument map(AnnotationDocumentEntity entity);

    public abstract List<ExternalObjectDirectoryCaseDocument> mapEods(List<ExternalObjectDirectoryEntity> entities);

    @Mappings({
        @Mapping(source = "transcriptionDocumentEntity", target = "transcriptionDocument"),
        @Mapping(source = "annotationDocumentEntity", target = "annotationDocument"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById"),
        @Mapping(target = "createdBy", source = "createdById")
    })
    public abstract ExternalObjectDirectoryCaseDocument mapEod(ExternalObjectDirectoryEntity entity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    public abstract TranscriptionCaseDocument.TranscriptionCommentCaseDocument mapTranscriptionCommentCaseDocument(TranscriptionCommentEntity entity);
}
