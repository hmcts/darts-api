package uk.gov.hmcts.darts.retention.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.test.common.data.CaseTestData;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseRetentionConfidenceReasonMapperTest {

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Test
    void mapToCaseRetentionConfidenceReason() {
        // given
        OffsetDateTime currentTime = OffsetDateTime.parse("2023-05-31T10:00:00+01:00");
        OffsetDateTime retentionDate = currentTime.plusYears(7);

        var courtCase1 = CaseTestData.createSomeMinimalCase();
        courtCase1.setRetConfUpdatedTs(retentionDate);
        courtCase1.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);

        var courtCase2 = CaseTestData.createSomeMinimalCase();
        courtCase2.setRetConfUpdatedTs(retentionDate);
        courtCase2.setRetConfReason(RetentionConfidenceReasonEnum.AGED_CASE);

        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ssX");
        var caseRetentionConfidenceReasonMapper = new CaseRetentionConfidenceReasonMapper(armDataManagementConfiguration);

        List<CourtCaseEntity> courtCases = List.of(courtCase1, courtCase2);

        //when
        var result = caseRetentionConfidenceReasonMapper.mapToCaseRetentionConfidenceReason(currentTime, courtCases);

        //then
        assertNotNull(result);
        assertEquals("2023-05-31T10:00:00+01", result.getRetentionConfidenceAppliedTimestamp());
        assertEquals(2, result.getRetentionCases().size());

        assertEquals(courtCase1.getCaseNumber(), result.getRetentionCases().get(0).getCaseNumber());
        assertEquals(courtCase1.getCourthouse().getCourthouseName(), result.getRetentionCases().get(0).getCourthouse());
        assertEquals("CASE_CLOSED", result.getRetentionCases().get(0).getRetentionConfidenceReason());
        assertEquals("2030-05-31T10:00:00+01", result.getRetentionCases().get(0).getRetentionConfidenceUpdatedTimestamp());

        assertEquals(courtCase2.getCaseNumber(), result.getRetentionCases().get(1).getCaseNumber());
        assertEquals(courtCase2.getCourthouse().getCourthouseName(), result.getRetentionCases().get(1).getCourthouse());
        assertEquals("AGED_CASE", result.getRetentionCases().get(1).getRetentionConfidenceReason());
        assertEquals("2030-05-31T10:00:00+01", result.getRetentionCases().get(1).getRetentionConfidenceUpdatedTimestamp());

    }
}