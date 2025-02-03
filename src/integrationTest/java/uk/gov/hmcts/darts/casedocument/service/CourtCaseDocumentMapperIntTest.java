package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.mapper.CourtCaseDocumentMapper;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:linelength")
class CourtCaseDocumentMapperIntTest extends IntegrationBase {

    @MockitoSpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @MockitoSpyBean
    CaseDocumentRepository caseDocumentRepository;
    @Autowired
    CourtCaseStub courtCaseStub;
    @Autowired
    UserIdentity userIdentity;
    @Autowired
    CourtCaseDocumentMapper mapper;

    @Test
    void mapToCourtCaseDocument() {
        // given
        ExternalObjectDirectoryEntity mediaEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByMedia(any())).thenReturn(List.of(mediaEodEntity));
        ExternalObjectDirectoryEntity transcriptionDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByTranscriptionDocumentEntity(any())).thenReturn(List.of(transcriptionDocumentEodEntity));
        ExternalObjectDirectoryEntity annotationDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByAnnotationDocumentEntity(any())).thenReturn(List.of(annotationDocumentEodEntity));
        CaseDocumentEntity caseDocumentEntity = dartsDatabase.getCaseDocumentStub().createCaseDocumentWithRandomValues();
        when(caseDocumentRepository.findByCourtCase(any())).thenReturn(List.of(caseDocumentEntity));
        ExternalObjectDirectoryEntity caseDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByCaseDocument(any())).thenReturn(List.of(caseDocumentEodEntity));

        CourtCaseEntity cc = courtCaseStub.createCourtCaseAndAssociatedEntitiesWithRandomValues();

        givenBearerTokenExists("darts.global.user@hmcts.net");

        // when
        CourtCaseDocument doc = mapper.mapToCaseDocument(cc);

        // then
        assertAll(
            "Grouped assertions for Case Document top level properties",
            () -> assertThat(doc.getCaseId()).isNotNull().isEqualTo(cc.getId()),
            () -> assertThat(doc.getCreatedBy()).isNotNull().isEqualTo(userIdentity.getUserAccount().getId()),
            () -> assertThat(doc.getCreatedDateTime()).isNotNull().isCloseToUtcNow(within(1, SECONDS)),
            () -> assertThat(doc.getLastModifiedBy()).isNotNull().isEqualTo(userIdentity.getUserAccount().getId()),
            () -> assertThat(doc.getLastModifiedDateTime()).isNotNull().isCloseToUtcNow(within(1, SECONDS)),
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
            () -> assertThat(doc.getReportingRestrictions().isReportingRestriction()).isNotNull().isEqualTo(
                cc.getReportingRestrictions().isReportingRestriction())
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
            () -> assertThat(doc.getDefendants().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getDefendantList().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getDefendants().get(0).getCreatedBy()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getDefendants().get(0).getLastModifiedBy()).isNotNull().isEqualTo(cc.getDefendantList().get(0).getLastModifiedBy().getId()),

            () -> assertThat(doc.getProsecutors().get(0).getId()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getId()),
            () -> assertThat(doc.getProsecutors().get(0).getName()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getName()),
            () -> assertThat(doc.getProsecutors().get(0).getCreatedDateTime()).isNotNull().isEqualTo(cc.getProsecutorList().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getProsecutors().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getProsecutorList().get(0).getLastModifiedDateTime()),
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
            () -> assertThat(doc.getCaseRetentions().get(0).getRetainUntilAppliedOn()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetainUntilAppliedOn()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCurrentState()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getCurrentState()),
            () -> assertThat(doc.getCaseRetentions().get(0).getComments()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().get(0).getComments()),
            () -> assertThat(doc.getCaseRetentions().get(0).getSubmittedBy()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getSubmittedBy().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionObjectId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getFixedPolicyKey()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getFixedPolicyKey()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getPolicyName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getPolicyName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getDisplayName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getDisplayName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getDuration()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getDuration()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getPolicyStart()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getPolicyStart()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getPolicyEnd()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getPolicyEnd()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getDescription()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getDescription()),
            () -> assertThat(doc.getCaseRetentions().get(0).getRetentionPolicyType().getRetentionPolicyObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getRetentionPolicyType().getRetentionPolicyObjectId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getTotalSentence()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getTotalSentence()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getFixedPolicyKey()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getFixedPolicyKey()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getPolicyName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getDisplayName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getDisplayName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getDuration()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getDuration()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getPolicyStart()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyStart()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getPolicyEnd()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyEnd()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getDescription()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getDescription()),
            () -> assertThat(
                doc.getCaseRetentions().get(0).getCaseManagementRetention().getRetentionPolicyType().getRetentionPolicyObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getRetentionPolicyTypeEntity().getRetentionPolicyObjectId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getLegacyObjectId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventText()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventText()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getTimestamp()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getTimestamp()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getLegacyVersionLabel()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getMessageId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getMessageId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().isLogEntry()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().isLogEntry()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getAntecedentId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getAntecedentId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getChronicleId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getChronicleId()),

            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getCreatedDateTime()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getCreatedBy()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getCreatedBy().getId()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getType()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getType()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getSubType()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getSubType()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getEventName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getEventName()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getHandler()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getHandler()),
            () -> assertThat(doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().getActive()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().getActive()),
            () -> assertThat(
                doc.getCaseRetentions().get(0).getCaseManagementRetention().getEvent().getEventType().isReportingRestriction()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().get(0).getCaseManagementRetention().getEventEntity().getEventType().isReportingRestriction())
        );

        assertAll(
            "Grouped assertions for Case Document hearings",
            () -> assertThat(doc.getHearings().get(0).getId()).isNotNull().isEqualTo(cc.getHearings().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getHearingDate()).isNotNull().isEqualTo(cc.getHearings().get(0).getHearingDate()),
            () -> assertThat(doc.getHearings().get(0).getScheduledStartTime()).isNotNull().isEqualTo(cc.getHearings().get(0).getScheduledStartTime()),
            () -> assertThat(doc.getHearings().get(0).getHearingIsActual()).isNotNull().isEqualTo(cc.getHearings().get(0).getHearingIsActual()),

            () -> assertThat(doc.getHearings().get(0).getCourtroom().getId()).isNotNull().isEqualTo(cc.getHearings().get(0).getCourtroom().getId()),
            () -> assertThat(doc.getHearings().get(0).getCourtroom().getName()).isNotNull().isEqualTo(cc.getHearings().get(0).getCourtroom().getName()),
            () -> assertThat(doc.getHearings().get(0).getCourtroom().getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getCourtroom().getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getCourtroom().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getCourtroom().getCreatedDateTime()),

            () -> assertThat(doc.getHearings().get(0).getJudges().get(0).getId()).isNotNull().isEqualTo(cc.getHearings().get(0).getJudges().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getJudges().get(0).getName()).isNotNull().isEqualTo(cc.getHearings().get(0).getJudges().get(0).getName()),
            () -> assertThat(doc.getHearings().get(0).getJudges().get(0).getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getJudges().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getJudges().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getJudges().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getJudges().get(0).getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getJudges().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getJudges().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getJudges().get(0).getLastModifiedBy().getId())
        );

        assertAll(
            "Grouped assertions for Case Document hearings media requests",
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getCurrentOwner()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getCurrentOwner().getId()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getRequestor()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getRequestor().getId()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getStatus()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getStatus()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getRequestType()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getRequestType()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getAttempts()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getAttempts()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getStartTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getStartTime()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getEndTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getEndTime()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMediaRequests().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaRequests().get(0).getLastModifiedBy().getId())
        );


        assertAll(
            "Grouped assertions for Case Document hearings medias",
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getId()).isNotNull().isEqualTo(cc.getHearings().get(0).getMediaList().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getLegacyObjectId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getChannel()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getChannel()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getTotalChannels()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getTotalChannels()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getStart()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getStart()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getEnd()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getEnd()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getMediaFile()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getMediaFile()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getMediaFormat()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getMediaFormat()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getChecksum()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getChecksum()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getFileSize()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getFileSize()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getMediaType()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getMediaType()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getContentObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getContentObjectId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getClipId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getClipId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getChronicleId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getChronicleId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAntecedentId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAntecedentId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).isHidden()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).isHidden()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).isDeleted()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).isDeleted()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getDeletedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getDeletedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getDeletedTimestamp()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getMediaStatus()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getMediaStatus()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getRetainUntilTs()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getRetainUntilTs()),

            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getAnnotationDocument()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getAnnotationDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getCaseDocument()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getMedia()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getMedia().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getTranscriptionDocument()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getTranscriptionDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getHiddenBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getHiddenBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getHiddenDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getHiddenDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).isMarkedForManualDeletion()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).isMarkedForManualDeletion()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getMarkedForManualDelBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getMarkedForManualDelBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getMarkedForManualDelDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getMarkedForManualDelDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getTicketReference()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getTicketReference()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getComments()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getComments()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getAdminActionReasons().get(0).getObjectHiddenReason()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getMediaList().get(0).getAdminActionReasons().get(0).getObjectHiddenReason()),

            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getId()).isNotNull().isEqualTo(
                mediaEodEntity.getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getMedia()).isNotNull().isEqualTo(
                mediaEodEntity.getMedia().getId()),
            () -> assertThat(
                doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getTranscriptionDocument()).isNotNull().isEqualTo(
                mediaEodEntity.getTranscriptionDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getAnnotationDocument()).isNotNull().isEqualTo(
                mediaEodEntity.getAnnotationDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getCaseDocument()).isNotNull().isEqualTo(
                mediaEodEntity.getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getStatus()).isNotNull().isEqualTo(
                mediaEodEntity.getStatus()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getExternalLocationType()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalLocationType()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getExternalLocation()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalLocation()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getExternalFileId()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalFileId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getExternalRecordId()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalRecordId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getChecksum()).isNotNull().isEqualTo(
                mediaEodEntity.getChecksum()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getTransferAttempts()).isNotNull().isEqualTo(
                mediaEodEntity.getTransferAttempts()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getVerificationAttempts()).isNotNull().isEqualTo(
                mediaEodEntity.getVerificationAttempts()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getManifestFile()).isNotNull().isEqualTo(
                mediaEodEntity.getManifestFile()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getEventDateTs()).isNotNull().isEqualTo(
                mediaEodEntity.getEventDateTs()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getErrorCode()).isNotNull().isEqualTo(
                mediaEodEntity.getErrorCode()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).isResponseCleaned()).isNotNull().isEqualTo(
                mediaEodEntity.isResponseCleaned()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).isUpdateRetention()).isNotNull().isEqualTo(
                mediaEodEntity.isUpdateRetention()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getCreatedBy()).isNotNull().isEqualTo(
                mediaEodEntity.getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getCreatedDateTime()).isNotNull().isEqualTo(
                mediaEodEntity.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                mediaEodEntity.getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getMedias().get(0).getExternalObjectDirectories().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                mediaEodEntity.getLastModifiedDateTime())
        );

        assertAll(
            "Grouped assertions for Case Document hearings transcriptions",
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getLegacyObjectId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionType()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionType()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionUrgency()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionUrgency()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionStatus()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionStatus()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getHearingDate()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getHearingDate()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getStartTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getStartTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getEndTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getEndTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getIsManualTranscription()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getIsManualTranscription()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getHideRequestFromRequestor()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getHideRequestFromRequestor()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getDeleted()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).isDeleted()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getChronicleId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getChronicleId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getAntecedentId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getAntecedentId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getDeletedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getDeletedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getDeletedTimestamp()),

            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getLastModifiedBy().getId()),
            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getLastModifiedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getLastModifiedTimestamp()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getClipId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getClipId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getFileName()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getFileName()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getFileSize()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getFileSize()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getFileType()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getFileType()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getUploadedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getUploadedBy().getId()),
            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getUploadedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getUploadedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getChecksum()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getChecksum()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getContentObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getContentObjectId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).isHidden()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).isHidden()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getRetainUntilTs()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getRetainUntilTs()),

            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getAnnotationDocument()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(
                    0).getAnnotationDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getCaseDocument()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(
                    0).getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getMedia()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getMedia().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getTranscriptionDocument()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(
                    0).getTranscriptionDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getHiddenBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getHiddenBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getHiddenDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getHiddenDateTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).isMarkedForManualDeletion()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(
                    0).isMarkedForManualDeletion()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getMarkedForManualDelBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(
                    0).getMarkedForManualDelBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getMarkedForManualDelDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(
                    0).getMarkedForManualDelDateTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getTicketReference()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getTicketReference()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getComments()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getComments()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getAdminActions().get(
                0).getObjectHiddenReason()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocumentEntities().get(0).getAdminActions().get(0).getObjectHiddenReason()),

            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getId()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getMedia()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getMedia().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getTranscriptionDocument()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getTranscriptionDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getAnnotationDocument()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getAnnotationDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getCaseDocument()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getStatus()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getStatus()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalLocationType()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalLocationType()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalLocation()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalLocation()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalFileId()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalFileId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalRecordId()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalRecordId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getChecksum()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getChecksum()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getTransferAttempts()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getTransferAttempts()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getVerificationAttempts()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getVerificationAttempts()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getManifestFile()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getManifestFile()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getEventDateTs()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getEventDateTs()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getErrorCode()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getErrorCode()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).isResponseCleaned()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.isResponseCleaned()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).isUpdateRetention()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.isUpdateRetention()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getCreatedBy()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getCreatedDateTime()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getLastModifiedBy()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionDocuments().get(0).getExternalObjectDirectories().get(
                0).getLastModifiedDateTime()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getLastModifiedDateTime()),

            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflows().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflowEntities().get(0).getId()),
            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflows().get(0).getTranscriptionStatus()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflowEntities().get(0).getTranscriptionStatus()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflows().get(0).getWorkflowActor()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflowEntities().get(0).getWorkflowActor().getId()),
            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflows().get(0).getWorkflowTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflowEntities().get(0).getWorkflowTimestamp()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflows().get(0).getTranscriptionComments().get(
                0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionWorkflowEntities().get(0).getTranscriptionComments().get(0).getId()),

            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionComments().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionCommentEntities().get(0).getId()),
            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionComments().get(0).getTranscriptionWorkflow()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionCommentEntities().get(0).getTranscriptionWorkflow().getId()),
            () -> assertThat(
                doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionComments().get(0).getLegacyTranscriptionObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionCommentEntities().get(0).getLegacyTranscriptionObjectId()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionComments().get(0).getComment()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionCommentEntities().get(0).getComment()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionComments().get(0).getCommentTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionCommentEntities().get(0).getCommentTimestamp()),
            () -> assertThat(doc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionComments().get(0).getAuthorUserId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getTranscriptions().get(0).getTranscriptionCommentEntities().get(0).getAuthorUserId())
        );

        assertAll(
            "Grouped assertions for Case Document hearings annotations",
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getLegacyObjectId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getDeletedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).isDeleted()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).isDeleted()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getDeletedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getDeletedTimestamp()),

            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getLastModifiedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getLastModifiedTimestamp()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getClipId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getClipId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getFileName()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getFileName()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getFileSize()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getFileSize()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getFileType()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getFileType()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getUploadedBy()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getUploadedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getUploadedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getUploadedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getChecksum()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getChecksum()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getContentObjectId()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getContentObjectId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).isHidden()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).isHidden()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getRetainUntilTs()).isNotNull().isEqualTo(
                cc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getRetainUntilTs()),

            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getId()).isNotNull().isEqualTo(annotationDocumentEodEntity.getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getMedia()).isNotNull().isEqualTo(annotationDocumentEodEntity.getMedia().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getTranscriptionDocument()).isNotNull().isEqualTo(annotationDocumentEodEntity.getTranscriptionDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getAnnotationDocument()).isNotNull().isEqualTo(annotationDocumentEodEntity.getAnnotationDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getCaseDocument()).isNotNull().isEqualTo(annotationDocumentEodEntity.getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getStatus()).isNotNull().isEqualTo(annotationDocumentEodEntity.getStatus()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalLocationType()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalLocationType()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalLocation()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalLocation()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalFileId()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalFileId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getExternalRecordId()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalRecordId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getChecksum()).isNotNull().isEqualTo(annotationDocumentEodEntity.getChecksum()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getTransferAttempts()).isNotNull().isEqualTo(annotationDocumentEodEntity.getTransferAttempts()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getVerificationAttempts()).isNotNull().isEqualTo(annotationDocumentEodEntity.getVerificationAttempts()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getManifestFile()).isNotNull().isEqualTo(annotationDocumentEodEntity.getManifestFile()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getEventDateTs()).isNotNull().isEqualTo(annotationDocumentEodEntity.getEventDateTs()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getErrorCode()).isNotNull().isEqualTo(annotationDocumentEodEntity.getErrorCode()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).isResponseCleaned()).isNotNull().isEqualTo(annotationDocumentEodEntity.isResponseCleaned()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).isUpdateRetention()).isNotNull().isEqualTo(annotationDocumentEodEntity.isUpdateRetention()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getCreatedBy()).isNotNull().isEqualTo(annotationDocumentEodEntity.getCreatedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getCreatedDateTime()).isNotNull().isEqualTo(annotationDocumentEodEntity.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getLastModifiedBy()).isNotNull().isEqualTo(annotationDocumentEodEntity.getLastModifiedBy().getId()),
            () -> assertThat(doc.getHearings().get(0).getAnnotations().get(0).getAnnotationDocuments().get(0).getExternalObjectDirectories().get(
                0).getLastModifiedDateTime()).isNotNull().isEqualTo(annotationDocumentEodEntity.getLastModifiedDateTime())
        );
    }

}
