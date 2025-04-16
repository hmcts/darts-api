package uk.gov.hmcts.darts.util.pagination;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class PaginationDto<T> {
    private Supplier<PaginatedList<T>> paginatedListSupplier;
    private Integer pageNumber;
    private Integer pageSize;
    List<String> sortBy;
    List<Sort.Direction> sortDirection;

    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public static List<Sort.Direction> toSortDirection(List<String> sortOrder) {
        if (sortOrder == null) {
            return null;
        }
        return sortOrder.stream()
            .map(string -> string.split(","))
            .flatMap(Arrays::stream)
            .map(Sort.Direction::fromString)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public static List<String> toSortBy(List<String> sortBy) {
        if (sortBy == null) {
            return null;
        }
        return sortBy.stream()
            .map(string -> string.split(","))
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    public boolean shouldPaginate() {
        return pageNumber != null && pageSize != null;
    }

    void setupAndValidate() {
        if (sortBy == null) {
            sortBy = new ArrayList<>();
        }
        if (sortDirection == null) {
            sortDirection = new ArrayList<>();
        }
        if (paginatedListSupplier == null) {
            paginatedListSupplier = PaginatedList::new;
        }
        //If sortBy and sortDirection are not null, then they should be of the same size
        if (sortBy != null && sortDirection != null && sortBy.size() != sortDirection.size()) {
            throw new DartsApiException(CommonApiError.INVALID_REQUEST, "sortBy and sortDirection must be of the same size");
        }
    }

    public Pageable toPagable(List<String> defaultSortOrder, List<Sort.Direction> defaultSortDirection,
                              Map<String, String> sortByMapper) {
        this.setupAndValidate();
        //Minus one from page number to convert to zero based index
        return PageRequest.of(this.getPageNumber() - 1, this.getPageSize(), this.getSort(defaultSortOrder, defaultSortDirection, sortByMapper));
    }

    Sort getSort(List<String> defaultSortBy, List<Sort.Direction> defaultSortDirection,
                 Map<String, String> sortByMapper) {
        List<String> combinedSortBy = new ArrayList<>(this.getSortBy());
        List<Sort.Direction> combinedSortDirections = new ArrayList<>(this.getSortDirection());

        //Add default sort order and direction to the end of the existing sort stack
        combinedSortBy.addAll(defaultSortBy);
        combinedSortDirections.addAll(defaultSortDirection);

        //Combined all sort orders and directions into a single list
        List<Sort.Order> sortOrders = new ArrayList<>();
        for (int index = 0; index < combinedSortBy.size(); index++) {
            String currentSortBy = combinedSortBy.get(index);
            currentSortBy = sortByMapper.getOrDefault(currentSortBy, currentSortBy);//Check if sortBy has a mapper else return default value
            Sort.Direction currentDirection = combinedSortDirections.get(index);
            sortOrders.add(new Sort.Order(currentDirection, currentSortBy));
        }
        return Sort.by(sortOrders);
    }


    public <I> PaginatedList<T> toPaginatedList(Function<Pageable, Page<I>> page,
                                                Function<I, T> dataMapper,
                                                List<String> sortOrder,
                                                List<Sort.Direction> sortDirection) {
        return toPaginatedList(page, dataMapper, sortOrder, sortDirection, Map.of());
    }

    public <I> PaginatedList<T> toPaginatedList(Function<Pageable, Page<I>> page,
                                                Function<I, T> dataMapper,
                                                List<String> sortOrder,
                                                List<Sort.Direction> sortDirection,
                                                Map<String, String> sortByMapper) {

        Page<I> results = page.apply(toPagable(sortOrder, sortDirection, sortByMapper));
        PaginatedList<T> paginatedList = paginatedListSupplier.get();
        paginatedList.setCurrentPage(this.getPageNumber());
        paginatedList.setPageSize(this.getPageSize());
        paginatedList.setTotalItems((int) results.getTotalElements(), this.getPageSize());
        paginatedList.setData(results.get().map(dataMapper).toList());

        return paginatedList;
    }
}
