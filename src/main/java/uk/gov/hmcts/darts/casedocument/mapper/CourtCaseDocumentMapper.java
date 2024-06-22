package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.casedocument.template.CaseDocumentCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.CaseRetentionCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.ExternalObjectDirectoryCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.HearingCaseDocument;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
    BasicCaseDocumentConversions.class,
    CaseObjectsCaseDocumentMapper.class,
    CaseDocumentRepository.class,
    ExternalObjectDirectoryRepository.class
})
public abstract class CourtCaseDocumentMapper {

    @Autowired
    CaseDocumentRepository caseDocumentRepository;
    @Autowired
    ExternalObjectDirectoryRepository eodRepository;

    @Mappings({
        @Mapping(source = "defendantList", target = "defendants"),
        @Mapping(source = "prosecutorList", target = "prosecutors"),
        @Mapping(source = "defenceList", target = "defences"),
        @Mapping(source = "caseRetentionEntities", target = "caseRetentions"),
        @Mapping(expression = "java(mapCaseDocuments(caseDocumentRepository.findByCourtCase(courtCase)))", target = "caseDocuments"),
    })
    public abstract CourtCaseDocument mapToCaseDocument(CourtCaseEntity courtCase);

    @Mappings({
        @Mapping(source = "retentionPolicyTypeEntity", target = "retentionPolicyType"),
        @Mapping(source = "eventEntity", target = "event"),
    })
    abstract CaseRetentionCaseDocument.CaseManagementRetentionCaseDocument mapToCaseDocument(CaseManagementRetentionEntity caseManagementRetentionEntity);

    @Mappings({
        @Mapping(source = "eventList", target = "events"),
        @Mapping(source = "mediaList", target = "medias"),
    })
    abstract HearingCaseDocument mapToCaseDocument(HearingEntity hearingEntity);

    @Mappings({
        @Mapping(expression = "java(mapCaseDocumentEods(eodRepository.findByCaseDocument(entity)))", target = "externalObjectDirectories"),
    })
    abstract CaseDocumentCaseDocument mapToCaseDocument(CaseDocumentEntity entity);

    public abstract List<CaseDocumentCaseDocument> mapCaseDocuments(List<CaseDocumentEntity> entities);

    abstract List<ExternalObjectDirectoryCaseDocument> mapCaseDocumentEods(List<ExternalObjectDirectoryEntity> entities);
}
