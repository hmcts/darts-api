package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.PostRetentionResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionPostServiceImplTest {


    @Captor
    ArgumentCaptor<CaseRetentionEntity> caseRetentionEntityArgumentCaptor;
    @InjectMocks
    private RetentionPostServiceImpl retentionPostService;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private AuditApi auditApi;
    @Mock
    private RetentionDateHelper retentionDateHelper;

    private void setupStubs() {
        CourtCaseEntity courtCase = CommonTestDataUtil.createCase("1");
        courtCase.setClosed(true);
        when(caseRepository.findById(1)).thenReturn(Optional.of(courtCase));

        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setCourtCase(courtCase);
        caseRetention.setCurrentState(CaseRetentionStatus.COMPLETE.name());
        caseRetention.setCreatedDateTime(OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        caseRetention.setRetainUntil(OffsetDateTime.of(2025, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        when(caseRetentionRepository.findLatestCompletedAutomatedRetention(any())).thenReturn(Optional.of(caseRetention));

        CaseRetentionEntity caseRetentionLater = new CaseRetentionEntity();
        caseRetentionLater.setCourtCase(courtCase);
        caseRetentionLater.setCurrentState(CaseRetentionStatus.COMPLETE.name());
        caseRetentionLater.setCreatedDateTime(OffsetDateTime.of(2024, 10, 1, 11, 0, 0, 0, ZoneOffset.UTC));
        caseRetentionLater.setRetainUntil(OffsetDateTime.of(2026, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyTypeManual = new RetentionPolicyTypeEntity();
        retentionPolicyTypeManual.setId(1);
        retentionPolicyTypeManual.setFixedPolicyKey("MANUAL");
        caseRetentionLater.setRetentionPolicyType(retentionPolicyTypeManual);
        when(caseRetentionRepository.findLatestCompletedRetention(any())).thenReturn(Optional.of(caseRetentionLater));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(eq(RetentionPolicyEnum.MANUAL.getPolicyKey()), any(OffsetDateTime.class))).thenReturn(
              Optional.of(retentionPolicyTypeManual));

        RetentionPolicyTypeEntity retentionPolicyTypePermanent = new RetentionPolicyTypeEntity();
        retentionPolicyTypePermanent.setId(2);
        retentionPolicyTypePermanent.setFixedPolicyKey("PERM");
        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
              eq(RetentionPolicyEnum.PERMANENT.getPolicyKey()),
              any(OffsetDateTime.class)
        )).thenReturn(
              Optional.of(retentionPolicyTypePermanent));

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setId(1);
        when(retentionPolicyTypeRepository.getReferenceById(anyInt())).thenReturn(retentionPolicyType);

        when(retentionDateHelper.getRetentionDateForPolicy(any(), any())).thenReturn(LocalDate.of(2123, 10, 1));
    }

    @Test
    void fail_NoCase() {
        setupStubs();
        when(caseRepository.findById(1)).thenReturn(Optional.empty());

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2026, 1, 1));
        postRetentionRequest.setComments("TheComments");

        var exception = assertThrows(
              DartsApiException.class,
              () -> retentionPostService.postRetention(postRetentionRequest)
        );

        assertEquals("The selected caseId '1' cannot be found.", exception.getDetail());
        assertEquals("RETENTION_103", exception.getError().getType().toString());
        assertEquals(400, exception.getError().getHttpStatus().value());
    }

    @Test
    void fail_CaseOpen() {
        setupStubs();
        CourtCaseEntity courtCase = CommonTestDataUtil.createCase("1");
        courtCase.setClosed(false);
        when(caseRepository.findById(1)).thenReturn(Optional.of(courtCase));

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2026, 1, 1));
        postRetentionRequest.setComments("TheComments");

        var exception = assertThrows(
              DartsApiException.class,
              () -> retentionPostService.postRetention(postRetentionRequest)
        );

        assertEquals("caseId '101' must be closed before the retention period can be amended.", exception.getDetail());
        assertEquals("RETENTION_104", exception.getError().getType().toString());
        assertEquals(400, exception.getError().getHttpStatus().value());
    }

    @Test
    void fail_NoCurrentRetention() {
        setupStubs();
        when(caseRetentionRepository.findLatestCompletedAutomatedRetention(any())).thenReturn(Optional.empty());

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2026, 1, 1));
        postRetentionRequest.setComments("TheComments");

        var exception = assertThrows(
              DartsApiException.class,
              () -> retentionPostService.postRetention(postRetentionRequest)
        );

        assertEquals("caseId '101' must have a retention policy applied before being changed.", exception.getDetail());
        assertEquals("RETENTION_105", exception.getError().getType().toString());
        assertEquals(400, exception.getError().getHttpStatus().value());
    }

    @Test
    void fail_BeforeAutomatedRetentionDate() {
        setupStubs();

        when(authorisationApi.userHasOneOfRoles(anyList())).thenReturn(true);

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2024, 1, 1));
        postRetentionRequest.setComments("TheComments");

        var exception = assertThrows(
              DartsApiException.class,
              () -> retentionPostService.postRetention(postRetentionRequest)
        );

        assertEquals("caseId '101' must have a retention date after the last completed automated retention date '2025-10-01'.", exception.getDetail());
        assertEquals("RETENTION_101", exception.getError().getType().toString());
        assertEquals(422, exception.getError().getHttpStatus().value());
    }

    @Test
    void fail_BeforeCurrentRetentionDate_NotJudge() {
        setupStubs();

        when(authorisationApi.userHasOneOfRoles(anyList())).thenReturn(false);

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2026, 1, 1));
        postRetentionRequest.setComments("TheComments");

        var exception = assertThrows(
              DartsApiException.class,
              () -> retentionPostService.postRetention(postRetentionRequest)
        );

        assertEquals("You do not have permission to reduce the retention period.", exception.getDetail());
        assertEquals("RETENTION_100", exception.getError().getType().toString());
        assertEquals(403, exception.getError().getHttpStatus().value());
    }

    @Test
    void ok_BeforeCurrentRetentionDate_Judge() {
        setupStubs();

        when(authorisationApi.userHasOneOfRoles(anyList())).thenReturn(true);

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2026, 1, 1));
        postRetentionRequest.setComments("TheComments");

        PostRetentionResponse response = retentionPostService.postRetention(postRetentionRequest);
        assertEquals("2026-01-01", response.getRetentionDate().toString());
        verify(caseRetentionRepository).saveAndFlush(caseRetentionEntityArgumentCaptor.capture());

        CaseRetentionEntity savedRetention = caseRetentionEntityArgumentCaptor.getValue();
        assertEquals("COMPLETE", savedRetention.getCurrentState());
        assertEquals("TheComments", savedRetention.getComments());
        assertEquals("2026-01-01T00:00Z", savedRetention.getRetainUntil().toString());
        assertEquals(10, savedRetention.getCreatedBy().getId());
        assertEquals(RetentionPolicyEnum.MANUAL.getPolicyKey(), savedRetention.getRetentionPolicyType().getFixedPolicyKey());

    }

    @Test
    void happy_path_increaseTime() {
        setupStubs();

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2027, 1, 1));
        postRetentionRequest.setComments("TheComments");
        PostRetentionResponse response = retentionPostService.postRetention(postRetentionRequest);
        assertEquals("2027-01-01", response.getRetentionDate().toString());

        verify(caseRetentionRepository).saveAndFlush(caseRetentionEntityArgumentCaptor.capture());

        CaseRetentionEntity savedRetention = caseRetentionEntityArgumentCaptor.getValue();
        assertEquals("COMPLETE", savedRetention.getCurrentState());
        assertEquals("TheComments", savedRetention.getComments());
        assertEquals("2027-01-01T00:00Z", savedRetention.getRetainUntil().toString());
        assertEquals(10, savedRetention.getCreatedBy().getId());
        assertEquals(RetentionPolicyEnum.MANUAL.getPolicyKey(), savedRetention.getRetentionPolicyType().getFixedPolicyKey());
    }

    @Test
    void happy_path_sameDateAsLastAutomated() {
        setupStubs();
        when(authorisationApi.userHasOneOfRoles(anyList())).thenReturn(true);

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2025, 10, 1));
        postRetentionRequest.setComments("TheComments");
        PostRetentionResponse response = retentionPostService.postRetention(postRetentionRequest);
        assertEquals("2025-10-01", response.getRetentionDate().toString());

        verify(caseRetentionRepository).saveAndFlush(caseRetentionEntityArgumentCaptor.capture());

        CaseRetentionEntity savedRetention = caseRetentionEntityArgumentCaptor.getValue();
        assertEquals("COMPLETE", savedRetention.getCurrentState());
        assertEquals("TheComments", savedRetention.getComments());
        assertEquals("2025-10-01T00:00Z", savedRetention.getRetainUntil().toString());
        assertEquals(10, savedRetention.getCreatedBy().getId());
        assertEquals(RetentionPolicyEnum.MANUAL.getPolicyKey(), savedRetention.getRetentionPolicyType().getFixedPolicyKey());
    }

    @Test
    void happy_path_increaseTime_validateOnly() {
        setupStubs();

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2027, 1, 1));
        postRetentionRequest.setComments("TheComments");
        postRetentionRequest.setValidateOnly(true);
        PostRetentionResponse response = retentionPostService.postRetention(postRetentionRequest);
        assertEquals("2027-01-01", response.getRetentionDate().toString());

        verify(caseRetentionRepository, never()).saveAndFlush(any());
    }

    @Test
    void happy_path_permanent() {
        setupStubs();

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setIsPermanentRetention(true);
        postRetentionRequest.setComments("TheComments");
        PostRetentionResponse response = retentionPostService.postRetention(postRetentionRequest);
        assertEquals("2123-10-01", response.getRetentionDate().toString());

        verify(caseRetentionRepository).saveAndFlush(caseRetentionEntityArgumentCaptor.capture());

        CaseRetentionEntity savedRetention = caseRetentionEntityArgumentCaptor.getValue();
        assertEquals("COMPLETE", savedRetention.getCurrentState());
        assertEquals("TheComments", savedRetention.getComments());
        assertEquals("2123-10-01T00:00Z", savedRetention.getRetainUntil().toString());
        assertEquals(10, savedRetention.getCreatedBy().getId());
        assertEquals(RetentionPolicyEnum.PERMANENT.getPolicyKey(), savedRetention.getRetentionPolicyType().getFixedPolicyKey());
    }

}
