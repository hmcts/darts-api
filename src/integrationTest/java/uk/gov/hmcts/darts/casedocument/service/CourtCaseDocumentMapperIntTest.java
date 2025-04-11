package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.mapper.CourtCaseDocumentMapper;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
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
        dartsDatabase.getCaseDocumentStub().createCaseDocumentWithRandomValues();
        ExternalObjectDirectoryEntity caseDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByCaseDocument(any())).thenReturn(List.of(caseDocumentEodEntity));

        CourtCaseEntity cc = courtCaseStub.createCourtCaseAndAssociatedEntitiesWithRandomValues();

        givenBearerTokenExists("darts.global.user@hmcts.net");
        cc.setLastModifiedBy(userIdentity.getUserAccount());
        cc.setCreatedBy(userIdentity.getUserAccount());

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
            () -> assertThat(doc.getReportingRestrictions().getCreatedBy()).isNotNull().isEqualTo(cc.getReportingRestrictions().getCreatedById()),
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
            () -> assertThat(doc.getCourthouse().getCreatedBy()).isNotNull().isEqualTo(cc.getCourthouse().getCreatedById()),
            () -> assertThat(doc.getCourthouse().getLastModifiedBy()).isNotNull().isEqualTo(cc.getCourthouse().getLastModifiedById()),
            () -> assertThat(doc.getCourthouse().getCode()).isNotNull().isEqualTo(cc.getCourthouse().getCode()),
            () -> assertThat(doc.getCourthouse().getDisplayName()).isNotNull().isEqualTo(cc.getCourthouse().getDisplayName()),
            () -> assertThat(doc.getCourthouse().getCourthouseName()).isNotNull().isEqualTo(cc.getCourthouse().getCourthouseName()),

            () -> assertThat(doc.getCourthouse().getRegion().getId()).isNotNull().isEqualTo(cc.getCourthouse().getRegion().getId()),
            () -> assertThat(doc.getCourthouse().getRegion().getRegionName()).isNotNull().isEqualTo(cc.getCourthouse().getRegion().getRegionName())
        );

        assertAll(
            "Grouped assertions for Case Document prosecutors, defendants, defences",
            () -> assertThat(doc.getDefendants().getFirst().getId()).isNotNull().isEqualTo(cc.getDefendantList().getFirst().getId()),
            () -> assertThat(doc.getDefendants().getFirst().getName()).isNotNull().isEqualTo(cc.getDefendantList().getFirst().getName()),
            () -> assertThat(doc.getDefendants().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(cc.getDefendantList().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getDefendants().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getDefendantList().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getDefendants().getFirst().getCreatedBy()).isNotNull().isEqualTo(cc.getDefendantList().getFirst().getCreatedById()),
            () -> assertThat(doc.getDefendants().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getDefendantList().getFirst().getLastModifiedById()),

            () -> assertThat(doc.getProsecutors().getFirst().getId()).isNotNull().isEqualTo(cc.getProsecutorList().getFirst().getId()),
            () -> assertThat(doc.getProsecutors().getFirst().getName()).isNotNull().isEqualTo(cc.getProsecutorList().getFirst().getName()),
            () -> assertThat(doc.getProsecutors().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getProsecutorList().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getProsecutors().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getProsecutorList().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getProsecutors().getFirst().getCreatedBy()).isNotNull().isEqualTo(cc.getProsecutorList().getFirst().getCreatedById()),
            () -> assertThat(doc.getProsecutors().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getProsecutorList().getFirst().getLastModifiedById()),

            () -> assertThat(doc.getDefences().getFirst().getId()).isNotNull().isEqualTo(cc.getDefenceList().getFirst().getId()),
            () -> assertThat(doc.getDefences().getFirst().getName()).isNotNull().isEqualTo(cc.getDefenceList().getFirst().getName()),
            () -> assertThat(doc.getDefences().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(cc.getDefenceList().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getDefences().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getDefenceList().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getDefences().getFirst().getCreatedBy()).isNotNull().isEqualTo(cc.getDefenceList().getFirst().getCreatedById()),
            () -> assertThat(doc.getDefences().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(cc.getDefenceList().getFirst().getLastModifiedById())
        );

        JudgeEntity judge = TestUtils.getFirst(cc.getJudges());
        assertAll(
            "Grouped assertions for Case Document judges",
            () -> assertThat(doc.getJudges().getFirst().getId()).isNotNull().isEqualTo(judge.getId()),
            () -> assertThat(doc.getJudges().getFirst().getName()).isNotNull().isEqualTo(judge.getName()),
            () -> assertThat(doc.getJudges().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(judge.getCreatedDateTime()),
            () -> assertThat(doc.getJudges().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(judge.getLastModifiedDateTime()),
            () -> assertThat(doc.getJudges().getFirst().getCreatedBy()).isNotNull().isEqualTo(judge.getCreatedById()),
            () -> assertThat(doc.getJudges().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(judge.getLastModifiedById())
        );

        assertAll(
            "Grouped assertions for Case Document retentions",
            () -> assertThat(doc.getCaseRetentions().getFirst().getId()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().getFirst().getId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getTotalSentence()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getTotalSentence()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetainUntil()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetainUntil()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetainUntilAppliedOn()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetainUntilAppliedOn()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCurrentState()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCurrentState()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getComments()).isNotNull().isEqualTo(cc.getCaseRetentionEntities().getFirst().getComments()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getSubmittedBy()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getSubmittedBy().getId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionObjectId()),

            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getFixedPolicyKey()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getFixedPolicyKey()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getPolicyName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getPolicyName()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getDisplayName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getDisplayName()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getDuration()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getDuration()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getPolicyStart()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getPolicyStart()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getPolicyEnd()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getPolicyEnd()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getDescription()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getDescription()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getRetentionPolicyType().getRetentionPolicyObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getRetentionPolicyType().getRetentionPolicyObjectId()),

            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getTotalSentence()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getTotalSentence()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getId()),
            () -> assertThat(
                doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getFixedPolicyKey()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getFixedPolicyKey()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getPolicyName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyName()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getDisplayName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getDisplayName()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getDuration()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getDuration()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getPolicyStart()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyStart()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getPolicyEnd()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getPolicyEnd()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getDescription()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getDescription()),
            () -> assertThat(
                doc.getCaseRetentions().getFirst().getCaseManagementRetention().getRetentionPolicyType().getRetentionPolicyObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getRetentionPolicyTypeEntity().getRetentionPolicyObjectId()),

            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getLegacyObjectId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventText()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventText()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getTimestamp()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getTimestamp()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getLegacyVersionLabel()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getMessageId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getMessageId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().isLogEntry()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().isLogEntry()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getAntecedentId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getAntecedentId()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getChronicleId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getChronicleId()),

            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getId()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getId()),
            () -> assertThat(
                doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getCreatedDateTime()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getCreatedBy()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getCreatedById()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getType()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getType()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getSubType()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getSubType()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getEventName()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getEventName()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getHandler()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getHandler()),
            () -> assertThat(doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().getActive()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().getActive()),
            () -> assertThat(
                doc.getCaseRetentions().getFirst().getCaseManagementRetention().getEvent().getEventType().isReportingRestriction()).isNotNull().isEqualTo(
                cc.getCaseRetentionEntities().getFirst().getCaseManagementRetention().getEventEntity().getEventType().isReportingRestriction())
        );

        JudgeEntity ccFirstHearingFirstJudge = TestUtils.getFirst(cc.getHearings().getFirst().getJudges());
        assertAll(
            "Grouped assertions for Case Document hearings",
            () -> assertThat(doc.getHearings().getFirst().getId()).isNotNull().isEqualTo(cc.getHearings().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getHearingDate()).isNotNull().isEqualTo(cc.getHearings().getFirst().getHearingDate()),
            () -> assertThat(doc.getHearings().getFirst().getScheduledStartTime()).isNotNull().isEqualTo(cc.getHearings().getFirst().getScheduledStartTime()),
            () -> assertThat(doc.getHearings().getFirst().getHearingIsActual()).isNotNull().isEqualTo(cc.getHearings().getFirst().getHearingIsActual()),

            () -> assertThat(doc.getHearings().getFirst().getCourtroom().getId()).isNotNull().isEqualTo(cc.getHearings().getFirst().getCourtroom().getId()),
            () -> assertThat(doc.getHearings().getFirst().getCourtroom().getName()).isNotNull().isEqualTo(cc.getHearings().getFirst().getCourtroom().getName()),
            () -> assertThat(doc.getHearings().getFirst().getCourtroom().getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getCourtroom().getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getCourtroom().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getCourtroom().getCreatedDateTime()),

            () -> assertThat(doc.getHearings().getFirst().getJudges().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstJudge.getId()),
            () -> assertThat(doc.getHearings().getFirst().getJudges().getFirst().getName()).isNotNull().isEqualTo(
                ccFirstHearingFirstJudge.getName()),
            () -> assertThat(doc.getHearings().getFirst().getJudges().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstJudge.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getJudges().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstJudge.getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getJudges().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstJudge.getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getJudges().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstJudge.getLastModifiedById())
        );

        assertAll(
            "Grouped assertions for Case Document hearings media requests",
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getCurrentOwner()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getCurrentOwner().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getRequestor()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getRequestor().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getStatus()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getStatus()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getRequestType()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getRequestType()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getAttempts()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getAttempts()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getStartTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getStartTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getEndTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getEndTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaRequests().getFirst().getLastModifiedById())
        );


        assertAll(
            "Grouped assertions for Case Document hearings medias",
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getLegacyObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getChannel()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getChannel()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getTotalChannels()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getTotalChannels()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getStart()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getStart()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getEnd()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getEnd()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaFile()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getMediaFile()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaFormat()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getMediaFormat()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getChecksum()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getChecksum()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getFileSize()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getFileSize()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaType()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getMediaType()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getContentObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getContentObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getClipId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getClipId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getChronicleId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getChronicleId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAntecedentId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getAntecedentId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().isHidden()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().isHidden()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().isDeleted()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().isDeleted()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getDeletedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getDeletedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getDeletedTimestamp()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaStatus()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getMediaStatus()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getRetainUntilTs()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getRetainUntilTs()),

            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getAnnotationDocument()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getAnnotationDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getCaseDocument()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getMedia()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getMedia().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getTranscriptionDocument()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getTranscriptionDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getHiddenBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getHiddenBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getHiddenDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getHiddenDateTime()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().isMarkedForManualDeletion()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().isMarkedForManualDeletion()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getMarkedForManualDelBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getMarkedForManualDelBy().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getMarkedForManualDelDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getMarkedForManualDelDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getTicketReference()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getTicketReference()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getComments()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getComments()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getObjectHiddenReason()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getMediaList().getFirst().getObjectAdminActions().getFirst().getObjectHiddenReason()),

            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getId()).isNotNull().isEqualTo(
                mediaEodEntity.getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getMedia()).isNotNull().isEqualTo(
                mediaEodEntity.getMedia().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getTranscriptionDocument()).isNotNull().isEqualTo(
                mediaEodEntity.getTranscriptionDocumentEntity().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getAnnotationDocument()).isNotNull().isEqualTo(
                mediaEodEntity.getAnnotationDocumentEntity().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getCaseDocument()).isNotNull().isEqualTo(
                mediaEodEntity.getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getStatus()).isNotNull().isEqualTo(
                mediaEodEntity.getStatus()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getExternalLocationType()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalLocationType()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getExternalLocation()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalLocation()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getExternalFileId()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalFileId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getExternalRecordId()).isNotNull().isEqualTo(
                mediaEodEntity.getExternalRecordId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getChecksum()).isNotNull().isEqualTo(
                mediaEodEntity.getChecksum()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getTransferAttempts()).isNotNull().isEqualTo(
                mediaEodEntity.getTransferAttempts()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getVerificationAttempts()).isNotNull().isEqualTo(
                mediaEodEntity.getVerificationAttempts()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getManifestFile()).isNotNull().isEqualTo(
                mediaEodEntity.getManifestFile()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getEventDateTs()).isNotNull().isEqualTo(
                mediaEodEntity.getEventDateTs()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getErrorCode()).isNotNull().isEqualTo(
                mediaEodEntity.getErrorCode()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().isResponseCleaned()).isNotNull().isEqualTo(
                mediaEodEntity.isResponseCleaned()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().isUpdateRetention()).isNotNull().isEqualTo(
                mediaEodEntity.isUpdateRetention()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                mediaEodEntity.getCreatedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                mediaEodEntity.getCreatedDateTime()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                mediaEodEntity.getLastModifiedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getExternalObjectDirectories().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                mediaEodEntity.getLastModifiedDateTime())
        );

        assertAll(
            "Grouped assertions for Case Document hearings transcriptions",
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getLegacyObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionType()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionType()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionUrgency()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionUrgency()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionStatus()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionStatus()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getHearingDate()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getHearingDate()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getStartTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getStartTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getEndTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getEndTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getIsManualTranscription()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getIsManualTranscription()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getHideRequestFromRequestor()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getHideRequestFromRequestor()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getDeleted()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().isDeleted()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getChronicleId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getChronicleId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getAntecedentId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getAntecedentId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getDeletedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getDeletedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getDeletedTimestamp()),

            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getLastModifiedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getLastModifiedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getLastModifiedTimestamp()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getClipId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getClipId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getFileName()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getFileName()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getFileSize()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getFileSize()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getFileType()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getFileType()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getUploadedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getUploadedBy().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getUploadedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getUploadedDateTime()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getChecksum()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getChecksum()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getContentObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getContentObjectId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().isHidden()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().isHidden()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getRetainUntilTs()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getRetainUntilTs()),

            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getAnnotationDocument()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getAnnotationDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getCaseDocument()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getMedia()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getMedia().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getTranscriptionDocument()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getTranscriptionDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getHiddenBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getHiddenBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getHiddenDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getHiddenDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).isMarkedForManualDeletion()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).isMarkedForManualDeletion()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getMarkedForManualDelBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getMarkedForManualDelBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getMarkedForManualDelDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getMarkedForManualDelDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getTicketReference()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getTicketReference()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getComments()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getComments()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getObjectHiddenReason()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getObjectHiddenReason()),

            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getId()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getMedia()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getMedia().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getTranscriptionDocument()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getTranscriptionDocumentEntity().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getAnnotationDocument()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getAnnotationDocumentEntity().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getCaseDocument()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getCaseDocument().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getStatus()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getStatus()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getExternalLocationType()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalLocationType()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getExternalLocation()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalLocation()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getExternalFileId()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalFileId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getExternalRecordId()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getExternalRecordId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getChecksum()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getChecksum()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getTransferAttempts()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getTransferAttempts()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getVerificationAttempts()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getVerificationAttempts()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getManifestFile()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getManifestFile()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getEventDateTs()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getEventDateTs()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getErrorCode()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getErrorCode()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).isResponseCleaned()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.isResponseCleaned()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).isUpdateRetention()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.isUpdateRetention()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getCreatedBy()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getCreatedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getCreatedDateTime()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getCreatedDateTime()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getLastModifiedBy()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getLastModifiedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getExternalObjectDirectories().get(
                    0).getLastModifiedDateTime()).isNotNull().isEqualTo(transcriptionDocumentEodEntity.getLastModifiedDateTime()),

            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflowEntities().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getTranscriptionStatus()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflowEntities().getFirst().getTranscriptionStatus()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getWorkflowActor()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflowEntities().getFirst().getWorkflowActor().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getWorkflowTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflowEntities().getFirst().getWorkflowTimestamp()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getTranscriptionComments().get(
                0).getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflowEntities().getFirst().getTranscriptionComments().getFirst().getId()),

            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionCommentEntities().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getTranscriptionWorkflow()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionCommentEntities().getFirst().getTranscriptionWorkflow().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getLegacyTranscriptionObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionCommentEntities().getFirst().getLegacyTranscriptionObjectId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getComment()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionCommentEntities().getFirst().getComment()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getCommentTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionCommentEntities().getFirst().getCommentTimestamp()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getAuthorUserId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionCommentEntities().getFirst().getAuthorUserId())
        );

        assertAll(
            "Grouped assertions for Case Document hearings annotations",
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLegacyObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getLegacyObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLegacyVersionLabel()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getDeletedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().isDeleted()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().isDeleted()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getDeletedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getDeletedTimestamp()),

            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getLastModifiedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getLastModifiedTimestamp()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getLastModifiedTimestamp()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getClipId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getClipId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileName()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileName()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileSize()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileSize()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileType()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileType()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getUploadedBy()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getUploadedBy().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getUploadedDateTime()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getUploadedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getChecksum()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getChecksum()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getContentObjectId()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getContentObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().isHidden()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().isHidden()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getRetainUntilTs()).isNotNull().isEqualTo(
                cc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getRetainUntilTs()),

            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getId()).isNotNull().isEqualTo(annotationDocumentEodEntity.getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getMedia()).isNotNull().isEqualTo(annotationDocumentEodEntity.getMedia().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getTranscriptionDocument()).isNotNull().isEqualTo(annotationDocumentEodEntity.getTranscriptionDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getAnnotationDocument()).isNotNull().isEqualTo(annotationDocumentEodEntity.getAnnotationDocumentEntity().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getCaseDocument()).isNotNull().isEqualTo(annotationDocumentEodEntity.getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getStatus()).isNotNull().isEqualTo(annotationDocumentEodEntity.getStatus()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getExternalLocationType()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalLocationType()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getExternalLocation()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalLocation()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getExternalFileId()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalFileId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getExternalRecordId()).isNotNull().isEqualTo(annotationDocumentEodEntity.getExternalRecordId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getChecksum()).isNotNull().isEqualTo(annotationDocumentEodEntity.getChecksum()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getTransferAttempts()).isNotNull().isEqualTo(annotationDocumentEodEntity.getTransferAttempts()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getVerificationAttempts()).isNotNull().isEqualTo(annotationDocumentEodEntity.getVerificationAttempts()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getManifestFile()).isNotNull().isEqualTo(annotationDocumentEodEntity.getManifestFile()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getEventDateTs()).isNotNull().isEqualTo(annotationDocumentEodEntity.getEventDateTs()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getErrorCode()).isNotNull().isEqualTo(annotationDocumentEodEntity.getErrorCode()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).isResponseCleaned()).isNotNull().isEqualTo(annotationDocumentEodEntity.isResponseCleaned()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).isUpdateRetention()).isNotNull().isEqualTo(annotationDocumentEodEntity.isUpdateRetention()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getCreatedBy()).isNotNull().isEqualTo(annotationDocumentEodEntity.getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getCreatedDateTime()).isNotNull().isEqualTo(annotationDocumentEodEntity.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getLastModifiedBy()).isNotNull().isEqualTo(annotationDocumentEodEntity.getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getExternalObjectDirectories().get(
                0).getLastModifiedDateTime()).isNotNull().isEqualTo(annotationDocumentEodEntity.getLastModifiedDateTime())
        );
    }

}
