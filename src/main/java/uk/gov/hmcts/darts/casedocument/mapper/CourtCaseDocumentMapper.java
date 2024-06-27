package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.template.CaseRetentionCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.HearingCaseDocument;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

@Mapper(componentModel = "spring", uses = {
    BasicCaseDocumentConversions.class,
    CaseObjectsCaseDocumentMapper.class,
    ExternalObjectDirectoryRepository.class,
    UserIdentity.class
})
public abstract class CourtCaseDocumentMapper {

    @Autowired
    ExternalObjectDirectoryRepository eodRepository;
    @Autowired
    UserIdentity userIdentity;

    @Mappings({
        @Mapping(target = "createdBy", qualifiedByName = "retrieveCaseDocumentGenerationUser"),
        @Mapping(target = "lastModifiedBy", qualifiedByName = "retrieveCaseDocumentGenerationUser"),
        @Mapping(target = "createdDateTime", expression = "java(OffsetDateTime.now())"),
        @Mapping(target = "lastModifiedDateTime", expression = "java(OffsetDateTime.now())"),
        @Mapping(source = "defendantList", target = "defendants"),
        @Mapping(source = "prosecutorList", target = "prosecutors"),
        @Mapping(source = "defenceList", target = "defences"),
        @Mapping(source = "caseRetentionEntities", target = "caseRetentions")
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

    @Named("retrieveCaseDocumentGenerationUser")
    protected Integer retrieveCaseDocumentGenerationUser(UserAccountEntity userAccountEntity) {
        return userIdentity.getUserAccount().getId();
    }
}
