package uk.gov.hmcts.darts.common.util.paginated;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import jakarta.validation.constraints.NotNull;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;
import java.util.function.Function;

public final class PaginationUtil {
    private PaginationUtil() {

    }

    public static <T, I> PaginatedList<T> toPaginatedList(JPQLQuery<I> query, IsPageable isPageable,
                                                          @NotNull SortMethod.HasComparableExpression defaultSortField,
                                                          @NotNull SortMethod defaultSortMethod,
                                                          Function<I, T> dataMapper) {
        return toPaginatedList(query, isPageable, defaultSortField, defaultSortMethod, dataMapper, null);
    }

    public static <T, I> PaginatedList<T> toPaginatedList(JPQLQuery<I> query, IsPageable isPageable,
                                                          @NotNull SortMethod.HasComparableExpression defaultSortField,
                                                          @NotNull SortMethod defaultSortMethod,
                                                          Function<I, T> dataMapper,
                                                          Long maxItems) {
        PaginatedList<T> paginatedList = new PaginatedList<>();
        paginatedList.setCurrentPage(isPageable.getPageNumber());
        query.limit(isPageable.getPageLimit())
            .offset(isPageable.getPageLimit() * (isPageable.getPageNumber() - 1));


        SortMethod sortMethod = Optional.ofNullable(isPageable.getSortMethod())
            .orElse(defaultSortMethod);

        if (isPageable.getSortField() == null) {
            query.orderBy(sortMethod.from(defaultSortField));
        } else {
            query.orderBy(sortMethod.from(isPageable.getSortField()));
            if (!isPageable.getSortField().equals(defaultSortField)) {
                query.orderBy(defaultSortMethod.from(defaultSortField));
            }
        }
        QueryResults<I> results = query.fetchResults();
        paginatedList.setTotalItems(results.getTotal(), isPageable.getPageLimit());
        paginatedList.setData(results.getResults().stream().map(dataMapper).toList());
        if (maxItems != null && paginatedList.getTotalItems() > maxItems) {
            throw new DartsApiException(
                CaseApiError.TOO_MANY_RESULTS,
                "A max of " + maxItems + " items can be returned but got " + paginatedList.getTotalItems());
        }
        return paginatedList;
    }
}
