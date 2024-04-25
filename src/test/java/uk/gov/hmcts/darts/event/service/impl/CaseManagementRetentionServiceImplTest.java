package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.event.service.impl.CourtLogsServiceImplTest.CASE_0000001;


@ExtendWith(MockitoExtension.class)
class CaseManagementRetentionServiceImplTest {

    static final String SWANSEA = "SWANSEA";

    @InjectMocks
    CaseManagementRetentionServiceImpl caseManagementRetentionService;

    @Mock
    CaseManagementRetentionRepository caseManagementRetentionRepository;

    @Mock
    RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Mock
    CurrentTimeHelper currentTimeHelper;

    @BeforeEach
    void setUp() {
        caseManagementRetentionService = new CaseManagementRetentionServiceImpl(
            caseManagementRetentionRepository,
            retentionPolicyTypeRepository,
            currentTimeHelper
        );
    }

    @Test
    void shouldCreateCaseManagementRetentionSuccessfully() {

        var hearingEntity = CommonTestDataUtil.createHearing(CASE_0000001, LocalTime.of(10, 0));
        EventEntity event = CommonTestDataUtil.createEventWith("LOG", "Test", hearingEntity);

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("99Y0M0D");

        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any())).thenReturn(List.of(retentionPolicyType));

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.caseRetentionFixedPolicy("3");
        retentionPolicy.setCaseTotalSentence("20Y3M4D");

        CaseManagementRetentionEntity caseManagementRetentionEntity =
            caseManagementRetentionService.createCaseManagementRetention(event, existingCaseEntity, retentionPolicy);

        verify(caseManagementRetentionRepository, times(1)).save(any());

        final CaseManagementRetentionEntity compare = caseManagementRetentionRepository.save(any());

        assertEquals(compare, caseManagementRetentionEntity);


    }


}
