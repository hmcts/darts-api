package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.casedocument.model.AnnotationCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.CaseRetentionCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.CourthouseCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.CourtroomCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.DefenceCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.DefendantCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.EventCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.EventHandlerCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.HearingCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.JudgeCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.MediaRequestCaseDocument;
import uk.gov.hmcts.darts.casedocument.model.ProsecutorCaseDocument;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

@Mapper(componentModel = "spring", uses = {
    BasicCaseDocumentConversions.class,
    CaseObjectsCaseDocumentMapper.class
})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public abstract class CourtCaseDocumentMapper {

    @Mappings({
        @Mapping(source = "id", target = "caseId"),
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById"),
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
        @Mapping(source = "events", target = "events"),
        @Mapping(source = "mediaList", target = "medias"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById"),
        @Mapping(target = "createdBy", source = "createdById")
    })
    abstract HearingCaseDocument mapToCaseDocument(HearingEntity hearingEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract DefendantCaseDocument mapToDefendantCaseDocument(DefendantEntity defendantEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract ProsecutorCaseDocument mapToProsecutorCaseDocument(ProsecutorEntity prosecutorEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract DefenceCaseDocument mapToDefenceCaseDocument(DefenceEntity defenceEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract CaseRetentionCaseDocument.RetentionPolicyTypeCaseDocument mapToRetentionPolicyTypeCaseDocument(
        RetentionPolicyTypeEntity retentionPolicyTypeEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract CaseRetentionCaseDocument mapToCaseRetentionCaseDocument(CaseRetentionEntity caseRetentionEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract JudgeCaseDocument mapToJudgeCaseDocument(JudgeEntity judgeEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract CourthouseCaseDocument mapToCourthouseCaseDocument(CourthouseEntity courthouseEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract EventCaseDocument mapToEventCaseDocument(EventEntity eventEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract MediaRequestCaseDocument mapToMediaRequestCaseDocument(MediaRequestEntity mediaRequestEntity);

    @Mappings({
        @Mapping(target = "createdBy", source = "createdById"),
        @Mapping(target = "lastModifiedBy", source = "lastModifiedById")
    })
    abstract AnnotationCaseDocument mapToAnnotationCaseDocument(AnnotationEntity annotationEntity);

    @Mapping(target = "createdBy", source = "createdById")
    abstract EventHandlerCaseDocument mapToEventHandlerCaseDocument(EventHandlerEntity eventHandlerEntity);

    @Mapping(target = "createdBy", source = "createdById")
    abstract CourtroomCaseDocument mapToCourtroomCaseDocument(CourtroomEntity courtroomEntity);

}
