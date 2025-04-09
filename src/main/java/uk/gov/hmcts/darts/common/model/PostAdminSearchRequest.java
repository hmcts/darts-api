package uk.gov.hmcts.darts.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostAdminSearchRequest {

    private @Nullable List<Integer> courthouseIds;
    private @Nullable String caseNumber;
    private @Nullable String courtroomName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private @Nullable LocalDate hearingStartAt;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private @Nullable LocalDate hearingEndAt;

}
