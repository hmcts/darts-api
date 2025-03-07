package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CaseManagementRetentionServiceImplTest {

    private static final String SWANSEA = "SWANSEA";
    private static final String CASE_0000001 = "Case0000001";

    private static final String ERROR_MESSAGE = """
        Data on the event could not be reconciled with Darts records. Could not find a retention policy for fixedPolicyKey '1000'""";

    private CaseManagementRetentionServiceImpl caseManagementRetentionService;

    @Mock
    private CaseManagementRetentionRepository caseManagementRetentionRepository;

    @Mock
    private RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

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

    @Test
    void shouldFailAndThrowInvalidRetentionPolicy() {

        var hearingEntity = CommonTestDataUtil.createHearing(CASE_0000001, LocalTime.of(10, 0));
        EventEntity event = CommonTestDataUtil.createEventWith("LOG", "Test", hearingEntity);

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("99Y0M0D");

        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any())).thenReturn(new ArrayList<>());

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.caseRetentionFixedPolicy("1000");
        retentionPolicy.setCaseTotalSentence("20Y3M4D");

        DartsApiException dartsApiException = assertThrows(DartsApiException.class,
                                                           () -> caseManagementRetentionService.createCaseManagementRetention(event, existingCaseEntity,
                                                                                                                              retentionPolicy));

        assertEquals(EventError.EVENT_DATA_NOT_FOUND, dartsApiException.getError());
        assertEquals(ERROR_MESSAGE, dartsApiException.getMessage());


    }


}
