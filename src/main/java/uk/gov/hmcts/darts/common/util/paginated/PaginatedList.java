package uk.gov.hmcts.darts.common.util.paginated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class PaginatedList<T> {

    @JsonProperty("current_page")
    @NotNull
    @Min(1)
    private Long currentPage;

    @JsonProperty("total_pages")
    @NotNull
    @Min(1)
    private Long totalPages;

    @JsonProperty("total_items")
    @NotNull
    @Min(0)
    private Long totalItems;

    @JsonProperty("data")
    private List<@NotNull T> data;


    public void setTotalItems(long totalItems, long pageLimit) {
        this.totalItems = totalItems;
        this.totalPages = this.totalItems / pageLimit
            + (this.totalItems % pageLimit == 0 ? 0 : 1);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }
}
