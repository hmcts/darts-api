package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.mapper.RetentionPolicyTypeMapper;
import uk.gov.hmcts.darts.retention.validation.CreatePolicyTypeValidator;
import uk.gov.hmcts.darts.retention.validation.EditPolicyTypeValidator;
import uk.gov.hmcts.darts.retention.validation.LivePolicyValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyDisplayNameIsUniqueValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyDurationValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyHasNoPendingRevisionValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyNameIsUniqueValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyStartDateIsFutureValidator;
import uk.gov.hmcts.darts.retention.validation.RevisePolicyTypeValidator;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicyType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.retention.exception.RetentionApiError.RETENTION_POLICY_TYPE_ID_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionGetPolicyTypesServiceImplTest {

    @Mock
    private RetentionPolicyTypeMapper retentionPolicyTypeMapper;

    @Mock
    private RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private PolicyDurationValidator policyDurationValidator;
    @Mock
    private CreatePolicyTypeValidator createPolicyTypeValidator;
    @Mock
    private EditPolicyTypeValidator editPolicyTypeValidator;
    @Mock
    private RevisePolicyTypeValidator revisePolicyTypeValidator;
    @Mock
    private PolicyNameIsUniqueValidator policyNameIsUniqueValidator;
    @Mock
    private PolicyDisplayNameIsUniqueValidator policyDisplayNameIsUniqueValidator;
    @Mock
    private PolicyStartDateIsFutureValidator policyStartDateIsFutureValidator;
    @Mock
    private LivePolicyValidator livePolicyValidator;
    @Mock
    private PolicyHasNoPendingRevisionValidator policyHasNoPendingRevisionValidator;
    @Mock
    private AuditApi auditApi;

    private RetentionPolicyTypeServiceImpl retentionService;

    private void setupStubs() {
        retentionService = new RetentionPolicyTypeServiceImpl(
            retentionPolicyTypeMapper,
            retentionPolicyTypeRepository,
            authorisationApi,
            policyDurationValidator,
            createPolicyTypeValidator,
            editPolicyTypeValidator,
            revisePolicyTypeValidator,
            policyNameIsUniqueValidator,
            policyDisplayNameIsUniqueValidator,
            policyStartDateIsFutureValidator,
            livePolicyValidator,
            policyHasNoPendingRevisionValidator,
            auditApi
        );

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
    }

    @Test
    void shouldSuccessfullyReturnListOfRetentionPolicyTypes() {
        setupStubs();

        when(retentionPolicyTypeRepository.findAll()).thenReturn(List.of(getRetentionPolicyTypeEntity()));

        when(retentionPolicyTypeMapper.mapToModelList(any())).thenReturn(List.of(createRetentionPolicyType()));

        List<RetentionPolicyType> caseRetentionPolicies = retentionService.getRetentionPolicyTypes();

        assertEquals(1, caseRetentionPolicies.getFirst().getId());

        verify(retentionPolicyTypeRepository).findAll(Sort.by(RetentionPolicyTypeEntity_.FIXED_POLICY_KEY).descending());
        verify(retentionPolicyTypeMapper).mapToModelList(any());
        verifyNoInteractions(editPolicyTypeValidator);
    }

    @Test
    void shouldSuccessfullyReturnPolicyType() {
        setupStubs();

        when(retentionPolicyTypeRepository.findById(anyInt())).thenReturn(Optional.of(getRetentionPolicyTypeEntity()));

        when(retentionPolicyTypeMapper.mapToModel(any())).thenReturn(createRetentionPolicyType());

        RetentionPolicyType caseRetentionPolicy = retentionService.getRetentionPolicyType(1);

        assertEquals(1, caseRetentionPolicy.getId());
        assertEquals("DARTS Permanent Retention v3", caseRetentionPolicy.getName());

        verify(retentionPolicyTypeRepository).findById(anyInt());
        verify(retentionPolicyTypeMapper).mapToModel(any());
        verifyNoInteractions(editPolicyTypeValidator);
    }

    @Test
    void shouldThrowDartsExceptionWhenPolicyTypeIdNotFound() {
        setupStubs();

        when(retentionPolicyTypeRepository.findById(anyInt())).thenThrow(new DartsApiException(RETENTION_POLICY_TYPE_ID_NOT_FOUND));

        var exception = assertThrows(
            DartsApiException.class,
            () -> retentionService.getRetentionPolicyType(123_456)
        );

        assertEquals(RETENTION_POLICY_TYPE_ID_NOT_FOUND.getTitle(), exception.getMessage());
        assertEquals(RETENTION_POLICY_TYPE_ID_NOT_FOUND, exception.getError());

        verifyNoInteractions(retentionPolicyTypeMapper);
        verifyNoInteractions(editPolicyTypeValidator);
    }

    private RetentionPolicyTypeEntity getRetentionPolicyTypeEntity() {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();


        retentionPolicyTypeEntity.setId(1);
        retentionPolicyTypeEntity.setDisplayName("Legacy Permanent");
        retentionPolicyTypeEntity.setPolicyName("DARTS Permanent Retention v3");
        retentionPolicyTypeEntity.setPolicyStart(OffsetDateTime.of(2024, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        retentionPolicyTypeEntity.setPolicyEnd(OffsetDateTime.of(2024, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        retentionPolicyTypeEntity.setDescription("DARTS Permanent retention policy");
        retentionPolicyTypeEntity.setFixedPolicyKey("-1");
        retentionPolicyTypeEntity.setDuration("30Y0M0D");
        retentionPolicyTypeEntity.setDescription("DARTS Permanent retention policy");
        return retentionPolicyTypeEntity;
    }

    private RetentionPolicyType createRetentionPolicyType() {

        RetentionPolicyType retentionPolicy = new RetentionPolicyType();
        retentionPolicy.setId(1);
        retentionPolicy.setDisplayName("Legacy Permanent");
        retentionPolicy.setName("DARTS Permanent Retention v3");
        retentionPolicy.setPolicyStartAt(OffsetDateTime.of(2024, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        retentionPolicy.setPolicyEndAt(OffsetDateTime.of(2024, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        retentionPolicy.setFixedPolicyKey("-1");
        retentionPolicy.setDuration("30Y0M0D");
        retentionPolicy.setDescription("DARTS Permanent retention policy");

        return retentionPolicy;
    }

}
