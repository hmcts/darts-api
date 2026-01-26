package uk.gov.hmcts.darts.retention.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionDateHelperTest {

    @Mock
    RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Mock
    CurrentTimeHelper currentTimeHelper;

    @Test
    void ok_99y() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("99Y0M0D");

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(List.of(retentionPolicyType));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);
        LocalDate response = retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT);

        assertEquals(LocalDate.of(2119, 10, 10), response);

    }

    @Test
    void getRetentionDateForPolicy_WithDefaultPolicy() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("7Y0M0D");

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(List.of(retentionPolicyType));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);
        LocalDate response = retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.DEFAULT);

        assertEquals(LocalDate.of(2027, 10, 10), response);

    }

    @Test
    void ok_99y9m9d() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("99Y9M9D");

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(List.of(retentionPolicyType));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);
        LocalDate response = retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT);

        assertEquals(LocalDate.of(2120, 7, 19), response);

    }

    @Test
    void ok_9m() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("9M");

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(List.of(retentionPolicyType));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);
        var exception = assertThrows(
            DartsApiException.class,
            () -> retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT)
        );

        assertEquals("PolicyString '9M', is not in the required format.", exception.getDetail());

    }

    @Test
    void ok_empty() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setDuration("");

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(List.of(retentionPolicyType));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);
        var exception = assertThrows(
            DartsApiException.class,
            () -> retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT)
        );

        assertEquals("PolicyString '', is not in the required format.", exception.getDetail());

    }

    @Test
    void ok_null() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType = new RetentionPolicyTypeEntity();

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(List.of(retentionPolicyType));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);

        var exception = assertThrows(
            DartsApiException.class,
            () -> retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT)
        );

        assertEquals("PolicyString 'null', is not in the required format.", exception.getDetail());
    }

    @Test
    void fail_multiplePolicies() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionPolicyTypeEntity retentionPolicyType1 = new RetentionPolicyTypeEntity();
        retentionPolicyType1.setDuration("99Y9M9D");

        RetentionPolicyTypeEntity retentionPolicyType2 = new RetentionPolicyTypeEntity();
        retentionPolicyType2.setDuration("7Y0M0D");

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(
            List.of(retentionPolicyType1, retentionPolicyType2));

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);

        var exception = assertThrows(
            DartsApiException.class,
            () -> retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT)
        );

        assertEquals("More than 1 retention policy found for fixedPolicyKey 'PERM'", exception.getDetail());
    }

    @Test
    void fail_NoPolicies() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        when(retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(anyString(), any(OffsetDateTime.class))).thenReturn(
            List.of());

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        RetentionDateHelper retentionDateHelper = new RetentionDateHelper(retentionPolicyTypeRepository, currentTimeHelper);

        var exception = assertThrows(
            DartsApiException.class,
            () -> retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.PERMANENT)
        );

        assertEquals("Cannot find Policy with FixedPolicyKey 'PERM'", exception.getDetail());
    }

}