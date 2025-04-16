package uk.gov.hmcts.darts.retention.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.retention.model.CaseRetentionConfidenceReason;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class CaseRetentionConfidenceReasonMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    public CaseRetentionConfidenceReason mapToCaseRetentionConfidenceReason(OffsetDateTime offsetDateTime, List<CourtCaseEntity> courtCases) {
        return CaseRetentionConfidenceReason.builder()
            .retentionConfidenceAppliedTimestamp(formatDateTime(offsetDateTime))
            .retentionCases(buildRetentionCases(courtCases))
            .build();
    }

    @SuppressWarnings("PMD.NullAssignment")
    private List<CaseRetentionConfidenceReason.RetentionCase> buildRetentionCases(List<CourtCaseEntity> courtCases) {
        List<CaseRetentionConfidenceReason.RetentionCase> retentionCases = new ArrayList<>();
        for (CourtCaseEntity courtCase : courtCases) {
            retentionCases.add(buildRetentionCase(courtCase));
        }
        if (retentionCases.isEmpty()) {
            retentionCases = null;
        }
        return retentionCases;
    }

    private CaseRetentionConfidenceReason.RetentionCase buildRetentionCase(CourtCaseEntity courtCase) {
        return CaseRetentionConfidenceReason.RetentionCase.builder()
            .courthouse(courtCase.getCourthouse().getCourthouseName())
            .caseNumber(courtCase.getCaseNumber())
            .retentionConfidenceUpdatedTimestamp(formatDateTime(courtCase.getRetConfUpdatedTs()))
            .retentionConfidenceReason(getCaseRetentionConfidenceReason(courtCase))
            .build();
    }

    private static String getCaseRetentionConfidenceReason(CourtCaseEntity courtCase) {
        String caseRetentionConfidenceReason = null;
        if (nonNull(courtCase.getRetConfReason())) {
            caseRetentionConfidenceReason = courtCase.getRetConfReason().name();
        }
        return caseRetentionConfidenceReason;
    }

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        String dateTime = null;
        if (nonNull(offsetDateTime)) {
            dateTime = offsetDateTime.format(dateTimeFormatter);
        }
        return dateTime;
    }
}
