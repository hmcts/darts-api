package uk.gov.hmcts.darts.cases.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import uk.gov.hmcts.darts.common.util.paginated.SortMethod;

@Getter
public class AdvancedSearchRequestPaginated extends AdvancedSearchRequest {

    @JsonProperty("sort_method")
    private SortMethod sortMethod;

    @JsonProperty("sort_field")
    private GetCasesSearchRequestPaginated.SortField sortField;

    @Min(1)
    @JsonProperty("page_limit")
    @Schema(name = "Page limit", description = "Number of items per page")
    private long pageLimit;

    @Min(1)
    @JsonProperty("page_number")
    @Schema(name = "Page number", description = "Page number to fetch (1-indexed)")
    private long pageNumber;
}
