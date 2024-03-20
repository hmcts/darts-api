package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.mapper.RetentionMapper;
import uk.gov.hmcts.darts.retention.mapper.RetentionPolicyMapper;
import uk.gov.hmcts.darts.retentions.model.GetRetentionPolicy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionGetPolicyTypesServiceImplTest {


    @Mock
    private CaseRetentionRepository caseRetentionRepository;

    @Mock
    private RetentionMapper retentionMapper;

    @Mock
    private RetentionPolicyMapper retentionPolicyMapper;

    @Mock
    private RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    @Mock
    private AuthorisationApi authorisationApi;

    private RetentionServiceImpl retentionService;


    private void setupStubs() {

        retentionService = new RetentionServiceImpl(
        caseRetentionRepository,
        retentionMapper,
        retentionPolicyMapper,
        retentionPolicyTypeRepository
        );

        when(retentionPolicyTypeRepository.findAll()).thenReturn(List.of());

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);


    }

    @Test
    void shouldSuccessfullyReturnListOfRetentionPolicyTypes() {
        setupStubs();

        when(retentionPolicyTypeRepository.findAll()).thenReturn(List.of(getRetentionPolicyTypeEntity()));

        when(retentionPolicyMapper.mapToRetentionPolicyResponse(any())).thenReturn(List.of(getRetentionPolicy()));

        List<GetRetentionPolicy> caseRetentionPolicies = retentionService.getRetentionPolicyTypes();

        assertEquals(1, caseRetentionPolicies.get(0).getId());

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

    private GetRetentionPolicy getRetentionPolicy() {

        GetRetentionPolicy getRetentionPolicy = new GetRetentionPolicy();
        getRetentionPolicy.setId(1);
        getRetentionPolicy.setDisplayName("Legacy Permanent");
        getRetentionPolicy.setName("DARTS Permanent Retention v3");
        getRetentionPolicy.setPolicyStartAt(OffsetDateTime.of(2024, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        getRetentionPolicy.setPolicyEndAt(OffsetDateTime.of(2024, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        getRetentionPolicy.setFixedPolicyKey("-1");
        getRetentionPolicy.setDuration("30Y0M0D");
        getRetentionPolicy.setDescription("DARTS Permanent retention policy");

        return getRetentionPolicy;
    }

}
