package uk.gov.hmcts.darts.casedocument.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.darts.testutils.CourtCaseTestData.createCourtCaseAndAssociatedEntitiesWithRandomValues;

class CourtCaseDocumentMapperTest {

    CourtCaseDocumentMapper mapper = Mappers.getMapper(CourtCaseDocumentMapper.class);

    @Test
    void mapToCourtCaseDocument() {

        CourtCaseEntity cc = createCourtCaseAndAssociatedEntitiesWithRandomValues();

        CourtCaseDocument doc = mapper.mapToCourtCaseDocument(cc);

        assertAll(
            "Grouped assertions for Case Document top level properties",
            () -> assertThat(doc.getId()).isNotNull().isEqualTo(cc.getId()),
            () -> assertThat(doc.getCreatedBy()).isNotNull().isEqualTo(cc.getCreatedBy().getId()),
            () -> assertThat(doc.getCreatedDateTime()).isNotNull().isEqualTo(cc.getCreatedDateTime()),
            () -> assertThat(doc.getLastModifiedBy()).isNotNull().isEqualTo(cc.getLastModifiedBy().getId()),
            () -> assertThat(doc.getLastModifiedDateTime()).isNotNull().isEqualTo(cc.getLastModifiedDateTime()),
            () -> assertThat(doc.getLegacyCaseObjectId()).isNotNull().isEqualTo(cc.getLegacyCaseObjectId()),
            () -> assertThat(doc.getCaseNumber()).isNotNull().isEqualTo(cc.getCaseNumber()),
            () -> assertThat(doc.getClosed()).isNotNull().isEqualTo(cc.getClosed()),
            () -> assertThat(doc.getInterpreterUsed()).isNotNull().isEqualTo(cc.getInterpreterUsed()),
            () -> assertThat(doc.getCaseClosedTimestamp()).isNotNull().isEqualTo(cc.getCaseClosedTimestamp()),
            () -> assertThat(doc.getDeletedBy()).isNotNull().isEqualTo(cc.getDeletedBy().getId()),
            () -> assertThat(doc.isDeleted()).isEqualTo(cc.isDeleted()),
            () -> assertThat(doc.getDeletedTimestamp()).isNotNull().isEqualTo(cc.getDeletedTimestamp()),
            () -> assertThat(doc.isRetentionUpdated()).isEqualTo(cc.isRetentionUpdated()),
            () -> assertThat(doc.isDataAnonymised()).isEqualTo(cc.isDataAnonymised()),
            () -> assertThat(doc.getDataAnonymisedBy()).isNotNull().isEqualTo(cc.getDataAnonymisedBy()),
            () -> assertThat(doc.getDataAnonymisedTs()).isNotNull().isEqualTo(cc.getDataAnonymisedTs())
        );

        assertAll(
            "Grouped assertions for Case Document reporting restrictions",
            () -> assertThat(doc.getReportingRestrictions().getId()).isNotNull().isEqualTo(cc.getReportingRestrictions().getId()),
            () -> assertThat(doc.getReportingRestrictions().getCreatedDateTime()).isNotNull().isEqualTo(cc.getReportingRestrictions().getCreatedDateTime()),
            () -> assertThat(doc.getReportingRestrictions().getCreatedBy()).isNotNull().isEqualTo(cc.getReportingRestrictions().getCreatedBy().getId()),
            () -> assertThat(doc.getReportingRestrictions().getType()).isNotNull().isEqualTo(cc.getReportingRestrictions().getType()),
            () -> assertThat(doc.getReportingRestrictions().getSubType()).isNotNull().isEqualTo(cc.getReportingRestrictions().getSubType()),
            () -> assertThat(doc.getReportingRestrictions().getEventName()).isNotNull().isEqualTo(cc.getReportingRestrictions().getEventName()),
            () -> assertThat(doc.getReportingRestrictions().getHandler()).isNotNull().isEqualTo(cc.getReportingRestrictions().getHandler()),
            () -> assertThat(doc.getReportingRestrictions().getActive()).isNotNull().isEqualTo(cc.getReportingRestrictions().getActive()),
            () -> assertThat(doc.getReportingRestrictions().getIsReportingRestriction()).isNotNull().isEqualTo(cc.getReportingRestrictions().getIsReportingRestriction())
        );

        assertAll(
            "Grouped assertions for Case Document courthouse",
            () -> assertThat(doc.getCourthouse().getId()).isNotNull().isEqualTo(cc.getCourthouse().getId()),
            () -> assertThat(doc.getCourthouse().getCreatedDateTime()).isNotNull().isEqualTo(cc.getCourthouse().getCreatedDateTime()),
            () -> assertThat(doc.getCourthouse().getLastModifiedDateTime()).isNotNull().isEqualTo(cc.getCourthouse().getLastModifiedDateTime()),
            () -> assertThat(doc.getCourthouse().getCreatedBy()).isNotNull().isEqualTo(cc.getCourthouse().getCreatedBy().getId()),
            () -> assertThat(doc.getCourthouse().getLastModifiedBy()).isNotNull().isEqualTo(cc.getCourthouse().getLastModifiedBy().getId()),
            () -> assertThat(doc.getCourthouse().getCode()).isNotNull().isEqualTo(cc.getCourthouse().getCode()),
            () -> assertThat(doc.getCourthouse().getDisplayName()).isNotNull().isEqualTo(cc.getCourthouse().getDisplayName()),
            () -> assertThat(doc.getCourthouse().getCourthouseName()).isNotNull().isEqualTo(cc.getCourthouse().getCourthouseName()),

            () -> assertThat(doc.getCourthouse().getRegion().getId()).isNotNull().isEqualTo(cc.getCourthouse().getRegion().getId()),
            () -> assertThat(doc.getCourthouse().getRegion().getRegionName()).isNotNull().isEqualTo(cc.getCourthouse().getRegion().getRegionName())
        );

        assertAll(
            "Grouped assertions for Case Document prosecutors, defendants, defences",
            () -> assertThat(doc.getDefendants().get(0).getId()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getId()),
            () -> assertThat(doc.getDefendants().get(0).getName()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getName()),
            () -> assertThat(doc.getDefendants().get(0).getCreatedDateTime()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getDefendants().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getDefendants().get(0).getCreatedBy()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getDefendants().get(0).getLastModifiedBy()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getLastModifiedBy().getId()),

            () -> assertThat(doc.getProsecutors().get(0).getId()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getId()),
            () -> assertThat(doc.getProsecutors().get(0).getName()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getName()),
            () -> assertThat(doc.getProsecutors().get(0).getCreatedDateTime()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getProsecutors().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getProsecutors().get(0).getCreatedBy()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getProsecutors().get(0).getLastModifiedBy()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getLastModifiedBy().getId()),

            () -> assertThat(doc.getDefences().get(0).getId()).isNotNull().isEqualTo(cc.getDefenceList().get(0).getId()),
            () -> assertThat(doc.getDefences().get(0).getName()).isNotNull().isEqualTo(cc.getDefenceList().get(0).getName()),
            () -> assertThat(doc.getDefences().get(0).getCreatedDateTime()).isNotNull().isEqualTo(cc.getDefenceList().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getDefences().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(cc.getDefenceList().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getDefences().get(0).getCreatedBy()).isNotNull().isEqualTo(cc.getDefenceList().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getDefences().get(0).getLastModifiedBy()).isNotNull().isEqualTo(cc.getDefenceList().get(0).getLastModifiedBy().getId())
        );

        assertAll(
            "Grouped assertions for Case Document judges",
            () -> assertThat(doc.getJudges().get(0).getId()).isNotNull().isEqualTo(cc.getJudges().get(0).getId()),
            () -> assertThat(doc.getJudges().get(0).getName()).isNotNull().isEqualTo(cc.getJudges().get(0).getName()),
            () -> assertThat(doc.getJudges().get(0).getCreatedDateTime()).isNotNull().isEqualTo(cc.getJudges().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getJudges().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(cc.getJudges().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getJudges().get(0).getCreatedBy()).isNotNull().isEqualTo(cc.getJudges().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getJudges().get(0).getLastModifiedBy()).isNotNull().isEqualTo(cc.getJudges().get(0).getLastModifiedBy().getId())
        );

        assertAll(
            "Grouped assertions for Case Document retentions",
            () -> assertThat(doc.getCaseRetentions().get(0).getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getTotalSentence()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getTotalSentence()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetainUntil()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetainUntil()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetainUntilAppliedOn()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetainUntilAppliedOn()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCurrentState()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCurrentState()),
            () -> assertThat(doc.getCaseRetentions().get(0).getComments()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getComments()),
            () -> assertThat(doc.getCaseRetentions().get(0).getSubmittedBy()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getSubmittedBy().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionObjectId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionObjectId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getFixedPolicyKey()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getFixedPolicyKey()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getPolicyName()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getPolicyName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getDisplayName()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getDisplayName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getDuration()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getDuration()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getPolicyStart()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getPolicyStart()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getPolicyEnd()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getPolicyEnd()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getDescription()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getDescription()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getRetentionPolicyObjectId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getRetentionPolicyObjectId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getTotalSentence()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getTotalSentence()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getFixedPolicyKey()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getFixedPolicyKey()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getPolicyName()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getDisplayName()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getDisplayName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getDuration()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getDuration()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getPolicyStart()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyStart()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getPolicyEnd()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyEnd()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getDescription()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getDescription()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getRetentionPolicyObjectId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getRetentionPolicyObjectId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getLegacyEventId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getLegacyEventId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getLegacyObjectId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getLegacyObjectId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventText()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventText()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getTimestamp()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getTimestamp()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getLegacyVersionLabel()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getLegacyVersionLabel()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getMessageId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getMessageId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getIsLogEntry()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getIsLogEntry()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getAntecedentId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getAntecedentId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getChronicleId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getChronicleId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getCreatedDateTime()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getCreatedDateTime()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getCreatedBy()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getCreatedBy().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getType()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getType()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getSubType()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getSubType()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getEventName()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getEventName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getHandler()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getHandler()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getActive()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getActive()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getIsReportingRestriction()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getIsReportingRestriction())
        );
    }
}