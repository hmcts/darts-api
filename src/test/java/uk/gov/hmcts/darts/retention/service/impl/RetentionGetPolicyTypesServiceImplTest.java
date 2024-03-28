package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.mapper.RetentionPolicyTypeMapper;
import uk.gov.hmcts.darts.retention.service.RetentionPolicyService;
import uk.gov.hmcts.darts.retention.validation.CreateOrRevisePolicyTypeValidator;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicy;

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
    private Validator<String> policyDurationValidator;
    @Mock
    private CreateOrRevisePolicyTypeValidator createOrRevisePolicyTypeValidator;
    @Mock
    private Validator<String> policyTypeRevisionValidator;
    @Mock
    private Validator<String> policyTypeCreationValidator;

    private RetentionPolicyService retentionService;


    private void setupStubs() {

        retentionService = new RetentionPolicyServiceImpl(
            retentionPolicyTypeMapper,
        retentionPolicyTypeRepository,
        authorisationApi,
        createOrRevisePolicyTypeValidator,
        policyDurationValidator,
        policyTypeRevisionValidator,
        policyTypeCreationValidator
        );

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);

    }

    @Test
    void shouldSuccessfullyReturnListOfRetentionPolicyTypes() {
        setupStubs();

        when(retentionPolicyTypeRepository.findAll()).thenReturn(List.of(getRetentionPolicyTypeEntity()));

        when(retentionPolicyTypeMapper.mapToModelList(any())).thenReturn(List.of(getRetentionPolicy()));

        List<RetentionPolicy> caseRetentionPolicies = retentionService.getRetentionPolicyTypes();

        assertEquals(1, caseRetentionPolicies.get(0).getId());

        verify(retentionPolicyTypeRepository).findAll();
        verify(retentionPolicyTypeMapper).mapToModelList(any());

    }

    @Test
    void shouldSuccessfullyReturnPolicyType() {
        setupStubs();

        when(retentionPolicyTypeRepository.findById(anyInt())).thenReturn(Optional.of(getRetentionPolicyTypeEntity()));

        when(retentionPolicyTypeMapper.mapToModel(any())).thenReturn(getRetentionPolicy());

        RetentionPolicy caseRetentionPolicy = retentionService.getRetentionPolicyType(1);

        assertEquals(1, caseRetentionPolicy.getId());
        assertEquals("DARTS Permanent Retention v3", caseRetentionPolicy.getName());

        verify(retentionPolicyTypeRepository).findById(anyInt());
        verify(retentionPolicyTypeMapper).mapToModel(any());

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

    private RetentionPolicy getRetentionPolicy() {

        RetentionPolicy retentionPolicy = new RetentionPolicy();
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
