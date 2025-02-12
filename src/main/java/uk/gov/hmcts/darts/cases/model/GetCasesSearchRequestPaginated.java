package uk.gov.hmcts.darts.cases.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.querydsl.core.types.Expression;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.common.entity.QHearingEntity;
import uk.gov.hmcts.darts.common.util.paginated.IsPageable;
import uk.gov.hmcts.darts.common.util.paginated.SortMethod;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GetCasesSearchRequestPaginated extends GetCasesSearchRequest implements IsPageable {

    @JsonProperty("sort_method")
    private SortMethod sortMethod;

    @JsonProperty("sort_field")
    private SortField sortField;

    @Min(1)
    @JsonProperty("page_limit")
    @Schema(name = "Page limit", description = "Number of items per page")
    private long pageLimit;

    @Min(1)
    @JsonProperty("page_number")
    @Schema(name = "Page number", description = "Page number to fetch (1-indexed)")
    private long pageNumber;


    @Getter
    public enum SortField implements SortMethod.HasComparableExpression {
        CASE_NUMBER(QHearingEntity.hearingEntity.courtCase.caseNumber),
        COURTHOUSE(QHearingEntity.hearingEntity.courtCase.courthouse.displayName);

        private final Expression<? extends Comparable<?>> comparableExpression;

        SortField(Expression<? extends Comparable<?>> comparableExpression) {
            this.comparableExpression = comparableExpression;
        }
    }
}
