package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.casedocument.mapper.CourtCaseDocumentMapper;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
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
        ExternalObjectDirectoryEntity caseDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByCaseDocument(any())).thenReturn(List.of(caseDocumentEodEntity));

        CourtCaseEntity cc = courtCaseStub.createCourtCaseAndAssociatedEntitiesWithRandomValues();

        // when
        CourtCaseDocument doc = mapper.mapToCaseDocument(cc);

        // then
        assertAll(
            "Grouped assertions for Case Document top level properties",
            () -> assertThat(doc.getCaseId()).isNotNull().isEqualTo(cc.getId()),
            () -> assertThat(doc.getCreatedBy()).isNotNull().isEqualTo(cc.getCreatedById()),
            () -> assertThat(doc.getCreatedDateTime()).isNotNull().isCloseToUtcNow(within(1, SECONDS)),
            () -> assertThat(doc.getLastModifiedBy()).isNotNull().isEqualTo(cc.getLastModifiedById()),
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

        JudgeEntity judge = TestUtils.getFirstInt(cc.getJudges());
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

        JudgeEntity ccFirstHearingFirstJudge = TestUtils.getFirstInt(cc.getHearings().getFirst().getJudges());
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

        MediaRequestEntity ccFirstHearingFirstMediaRequest = TestUtils.getFirstInt(cc.getHearings().getFirst().getMediaRequests());
        assertAll(
            "Grouped assertions for Case Document hearings media requests",
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getId()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getCurrentOwner()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getCurrentOwner().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getRequestor()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getRequestor().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getStatus()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getStatus()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getRequestType()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getRequestType()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getAttempts()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getAttempts()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getStartTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getStartTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getEndTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getEndTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getMediaRequests().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMediaRequest.getLastModifiedById())
        );


        MediaEntity ccFirstHearingFirstMedia = TestUtils.getFirstLong(cc.getHearings().getFirst().getMedias());
        assertAll(
            "Grouped assertions for Case Document hearings medias",
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLegacyObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getLegacyObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getChannel()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getChannel()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getTotalChannels()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getTotalChannels()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getStart()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getStart()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getEnd()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getEnd()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getLegacyVersionLabel()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaFile()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getMediaFile()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaFormat()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getMediaFormat()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getChecksum()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getChecksum()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getFileSize()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getFileSize()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaType()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getMediaType()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getContentObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getContentObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getClipId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getClipId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getChronicleId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getChronicleId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAntecedentId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getAntecedentId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().isHidden()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.isHidden()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().isDeleted()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.isDeleted()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getDeletedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getDeletedTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getDeletedTimestamp()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getMediaStatus()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getMediaStatus()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getRetainUntilTs()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getRetainUntilTs()),

            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getAnnotationDocument()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getAnnotationDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getCaseDocument()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getMedia()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getMedia().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getTranscriptionDocument()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getTranscriptionDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getHiddenBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getHiddenBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getHiddenDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getHiddenDateTime()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().isMarkedForManualDeletion()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().isMarkedForManualDeletion()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getMarkedForManualDelBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getMarkedForManualDelBy().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getMarkedForManualDelDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getMarkedForManualDelDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getTicketReference()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getTicketReference()),
            () -> assertThat(doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getComments()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getComments()),
            () -> assertThat(
                doc.getHearings().getFirst().getMedias().getFirst().getAdminActionReasons().getFirst().getObjectHiddenReason()).isNotNull().isEqualTo(
                ccFirstHearingFirstMedia.getObjectAdminActions().getFirst().getObjectHiddenReason()),

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

        TranscriptionEntity ccFirstHearingFirstTranscription = TestUtils.getFirstLong(cc.getHearings().getFirst().getTranscriptions());
        assertAll(
            "Grouped assertions for Case Document hearings transcriptions",
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLegacyObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getLegacyObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionType()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionType()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionUrgency()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionUrgency()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionStatus()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionStatus()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getHearingDate()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getHearingDate()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getLegacyVersionLabel()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getStartTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getStartTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getEndTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getEndTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getIsManualTranscription()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getIsManualTranscription()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getHideRequestFromRequestor()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getHideRequestFromRequestor()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getDeleted()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.isDeleted()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getChronicleId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getChronicleId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getAntecedentId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getAntecedentId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getDeletedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getDeletedTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getDeletedTimestamp()),

            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getLastModifiedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getLastModifiedTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getLastModifiedTimestamp()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getClipId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getClipId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getFileName()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getFileName()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getFileSize()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getFileSize()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getFileType()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getFileType()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getUploadedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getUploadedBy().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getUploadedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getUploadedDateTime()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getChecksum()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getChecksum()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getContentObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getContentObjectId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().isHidden()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().isHidden()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getRetainUntilTs()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getRetainUntilTs()),

            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getAnnotationDocument()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getAnnotationDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getCaseDocument()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getCaseDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getMedia()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getMedia().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getTranscriptionDocument()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getTranscriptionDocument().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getHiddenBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getHiddenBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getHiddenDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getHiddenDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).isMarkedForManualDeletion()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).isMarkedForManualDeletion()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getMarkedForManualDelBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getMarkedForManualDelBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getMarkedForManualDelDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().get(
                    0).getMarkedForManualDelDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getTicketReference()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getTicketReference()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getComments()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getComments()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionDocuments().getFirst().getAdminActions().get(
                0).getObjectHiddenReason()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionDocumentEntities().getFirst().getAdminActions().getFirst().getObjectHiddenReason()),

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
                ccFirstHearingFirstTranscription.getTranscriptionWorkflowEntities().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getTranscriptionStatus()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionWorkflowEntities().getFirst().getTranscriptionStatus()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getWorkflowActor()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionWorkflowEntities().getFirst().getWorkflowActor().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getWorkflowTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionWorkflowEntities().getFirst().getWorkflowTimestamp()),
            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionWorkflows().getFirst().getTranscriptionComments().get(
                0).getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionWorkflowEntities().getFirst().getTranscriptionComments().getFirst().getId()),

            () -> assertThat(doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionCommentEntities().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getTranscriptionWorkflow()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionCommentEntities().getFirst().getTranscriptionWorkflow().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getLegacyTranscriptionObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionCommentEntities().getFirst().getLegacyTranscriptionObjectId()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getComment()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionCommentEntities().getFirst().getComment()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getCommentTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionCommentEntities().getFirst().getCommentTimestamp()),
            () -> assertThat(
                doc.getHearings().getFirst().getTranscriptions().getFirst().getTranscriptionComments().getFirst().getAuthorUserId()).isNotNull().isEqualTo(
                ccFirstHearingFirstTranscription.getTranscriptionCommentEntities().getFirst().getAuthorUserId())
        );

        AnnotationEntity ccFirstHearingFirstAnnotation = TestUtils.getFirstInt(cc.getHearings().getFirst().getAnnotations());
        assertAll(
            "Grouped assertions for Case Document hearings annotations",
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getCreatedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getCreatedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLastModifiedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getLastModifiedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getCreatedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getCreatedById()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getLastModifiedById()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLegacyObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getLegacyObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getLegacyVersionLabel()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getLegacyVersionLabel()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getDeletedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getDeletedBy().getId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().isDeleted()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.isDeleted()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getDeletedTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getDeletedTimestamp()),

            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getId()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getLastModifiedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getLastModifiedById()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getLastModifiedTimestamp()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getLastModifiedTimestamp()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getClipId()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getClipId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileName()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getFileName()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileSize()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getFileSize()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getFileType()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getFileType()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getUploadedBy()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getUploadedBy().getId()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getUploadedDateTime()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getUploadedDateTime()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getChecksum()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getChecksum()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getContentObjectId()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getContentObjectId()),
            () -> assertThat(doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().isHidden()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().isHidden()),
            () -> assertThat(
                doc.getHearings().getFirst().getAnnotations().getFirst().getAnnotationDocuments().getFirst().getRetainUntilTs()).isNotNull().isEqualTo(
                ccFirstHearingFirstAnnotation.getAnnotationDocuments().getFirst().getRetainUntilTs()),

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
