package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import uk.gov.hmcts.darts.casedocument.template.CaseRetentionCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.HearingCaseDocument;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Mapper(componentModel = "spring")
public interface CourtCaseDocumentMapper {

    @Mappings({
        @Mapping(source = "defendantList", target = "defendants"),
        @Mapping(source = "prosecutorList", target = "prosecutors"),
        @Mapping(source = "defenceList", target = "defences"),
        @Mapping(source = "caseRetentionEntities", target = "caseRetentions"),
    })
    CourtCaseDocument map(CourtCaseEntity courtCase);

    @Mappings({
        @Mapping(source = "retentionPolicyTypeEntity", target = "retentionPolicyType"),
        @Mapping(source = "eventEntity", target = "event"),
    })
    CaseRetentionCaseDocument.CaseManagementRetentionCaseDocument map(CaseManagementRetentionEntity caseManagementRetentionEntity);

    @Mappings({
        @Mapping(source = "eventList", target = "events"),
        @Mapping(source = "mediaList", target = "medias"),
    })
    HearingCaseDocument map(HearingEntity hearingEntity);

    default Integer convertUserAccountToId(UserAccountEntity userAccountEntity) {
        return userAccountEntity.getId();
    }
}
