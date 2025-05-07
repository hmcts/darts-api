package uk.gov.hmcts.darts.util.pagination;

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
import org.apache.commons.collections.CollectionUtils;
import uk.gov.hmcts.darts.cases.model.PaginatedListCommon;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;
import java.util.function.BiConsumer;

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
    private Integer currentPage;

    @JsonProperty("total_pages")
    @NotNull
    @Min(1)
    private Integer totalPages;

    @JsonProperty("page_size")
    @NotNull
    @Min(1)
    private Integer pageSize;

    @JsonProperty("total_items")
    @NotNull
    @Min(0)
    private Integer totalItems;

    @JsonProperty("data")
    private List<@NotNull T> data;


    public void setTotalItems(int totalItems, int pageLimit) {
        this.totalItems = totalItems;
        this.totalPages = this.totalItems / pageLimit
            + (this.totalItems % pageLimit == 0 ? 0 : 1);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(data);
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")//False positive
    public <T> T asClass(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            return (T) this;
        }
        throw new DartsApiException(
            CommonApiError.INTERNAL_SERVER_ERROR,
            "Invalid class type provided for conversion: " + clazz.getName() +
                ". Expected: " + this.getClass().getName() + "."
        );
    }

    public <P extends PaginatedListCommon> P mapToPaginatedListCommon(P paginatedListCommon, BiConsumer<P, List<T>> dataConsumer) {
        paginatedListCommon.setCurrentPage(currentPage);
        paginatedListCommon.setPageSize(pageSize);
        paginatedListCommon.setTotalPages(totalPages);
        paginatedListCommon.setTotalItems(totalItems);
        dataConsumer.accept(paginatedListCommon, data);
        return paginatedListCommon;
    }
}
