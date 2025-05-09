package uk.gov.hmcts.darts.util.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")//Caused by mockito this can never be incorrect
class PaginationDtoTest {

    private PaginationDto<String> createValidObject() {
        return spy(new PaginationDto<>(
            PaginatedList::new,
            2,
            10,
            List.of("ABC", "ZXC"),
            List.of(Sort.Direction.ASC, Sort.Direction.DESC)
        ));
    }

    @Test
    void toSortDirection_whenNullIsProvided_nullShouldBeRetured() {
        assertThat(PaginationDto.toSortDirection(null)).isNull();
    }

    @Test
    void toSortDirection_whenValuesAreProvided_shouldReturnMappedList() {
        assertThat(PaginationDto.toSortDirection(List.of("ASC", "DESC")))
            .hasSize(2)
            .containsExactly(Sort.Direction.ASC, Sort.Direction.DESC);
    }

    @Test
    void toSortDirection_whenValuesAreProvidedCommaSeperated_shouldReturnMappedList() {
        assertThat(PaginationDto.toSortDirection(List.of("ASC,DESC")))
            .hasSize(2)
            .containsExactly(Sort.Direction.ASC, Sort.Direction.DESC);
    }

    @Test
    void toSortDirection_whenValuesAreProvidedCommaSeperatedAndStandard_shouldReturnMappedList() {
        assertThat(PaginationDto.toSortDirection(List.of("ASC,DESC", "DESC")))
            .hasSize(3)
            .containsExactly(Sort.Direction.ASC, Sort.Direction.DESC, Sort.Direction.DESC);
    }

    @Test
    void toSortBy_whenNullIsProvided_nullShouldBeRetured() {
        assertThat(PaginationDto.toSortBy(null)).isNull();
    }

    @Test
    void toSortBy_whenValuesAreProvided_shouldReturnMappedList() {
        assertThat(PaginationDto.toSortBy(List.of("ABC", "ZXC")))
            .hasSize(2)
            .containsExactly("ABC", "ZXC");
    }

    @Test
    void toSortBy_whenValuesAreProvidedCommaSeperated_shouldReturnMappedList() {
        assertThat(PaginationDto.toSortBy(List.of("ABC,ZXC")))
            .hasSize(2)
            .containsExactly("ABC", "ZXC");
    }

    @Test
    void toSortBy_whenValuesAreProvidedCommaSeperatedAndStandard_shouldReturnMappedList() {
        assertThat(PaginationDto.toSortBy(List.of("ABC,ZXC", "QWERTY")))
            .hasSize(3)
            .containsExactly("ABC", "ZXC", "QWERTY");
    }

    @Test
    void shouldPaginate_whenPageNumberAndPageSizeAreProvided_shouldReturnTrue() {
        PaginationDto<String> paginationDto = new PaginationDto<>(
            null,
            1,
            10,
            null,
            null
        );
        assertThat(paginationDto.shouldPaginate()).isTrue();
    }

    @Test
    void shouldPaginate_whenPageNumberAndPageSizeAreNotProvided_shouldReturnFalse() {
        PaginationDto<String> paginationDto = new PaginationDto<>(
            null,
            null,
            null,
            null,
            null
        );
        assertThat(paginationDto.shouldPaginate()).isFalse();
    }

    @Test
    void setupAndValidate_nullSortByAndSortDirection_shouldSetEmptyList() {
        PaginationDto<String> paginationDto = new PaginationDto<>(
            null,
            null,
            null,
            null,
            null
        );
        paginationDto.setupAndValidate();
        assertThat(paginationDto.getSortBy()).isEmpty();
        assertThat(paginationDto.getSortDirection()).isEmpty();
    }

    @Test
    void setupAndValidate_nullPaginatedListSupplier_shouldSetDefault() {
        PaginationDto<String> paginationDto = new PaginationDto<>(
            null,
            null,
            null,
            null,
            null
        );
        paginationDto.setupAndValidate();
        assertThat(paginationDto.getPaginatedListSupplier()).isNotNull();
        assertThat(paginationDto.getPaginatedListSupplier().get()).isEqualTo(new PaginatedList<>());
    }

    @Test
    void setupAndValidate_sortByAndSortDirectionOfDifferentSize_shouldThrowException() {
        PaginationDto<String> paginationDto = new PaginationDto<>(
            null,
            null,
            null,
            List.of("ASC"),
            List.of(Sort.Direction.ASC, Sort.Direction.DESC)
        );
        DartsApiException exception = assertThrows(DartsApiException.class, () -> paginationDto.setupAndValidate());
        assertThat(exception.getError()).isEqualTo(CommonApiError.INVALID_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("Invalid request. sortBy and sortDirection must be of the same size");
    }

    @Test
    void toPagable_whenProvidedStandardData_shouldReturnCorrectPageable() {
        List<String> defaultSortOrder = List.of("defaultSortField");
        List<Sort.Direction> defaultSortDirection = List.of(Sort.Direction.ASC);
        Map<String, String> sortByMapper = Map.of("defaultSortField", "defaultSortField");
        PaginationDto<String> paginationDto = createValidObject();

        doNothing().when(paginationDto).setupAndValidate();
        Sort sort = Sort.unsorted();
        doReturn(sort).when(paginationDto).getSort(any(), any(), any());

        assertThat(paginationDto.toPagable(defaultSortOrder, defaultSortDirection, sortByMapper))
            .isEqualTo(PageRequest.of(1, 10, sort));

        verify(paginationDto).setupAndValidate();
        verify(paginationDto).getSort(
            defaultSortOrder,
            defaultSortDirection,
            sortByMapper
        );
    }

    @Test
    void getSort_hasNoSortByOverrides_shouldReturnDefaultSort() {
        PaginationDto<String> paginationDto = createValidObject();
        paginationDto.sortBy = List.of();
        paginationDto.sortDirection = List.of();
        Sort sort = paginationDto.getSort(
            List.of("123D"),
            List.of(Sort.Direction.ASC),
            Map.of("123D", "D321"));
        assertThat(sort).isEqualTo(Sort.by(Sort.Direction.ASC, "D321"));
    }

    @Test
    void getSort_hasSortOverrides_shouldSortByOverridesThenDefaultSort() {
        PaginationDto<String> paginationDto = createValidObject();
        Sort sort = paginationDto.getSort(
            List.of("123D", "312"),
            List.of(Sort.Direction.ASC, Sort.Direction.ASC),
            Map.of("123D", "D321",
                   "ABC", "CBA"));
        assertThat(sort).isEqualTo(
            Sort.by(
                new Sort.Order(Sort.Direction.ASC, "CBA"),//From override... ABC gets mapped to CBA
                new Sort.Order(Sort.Direction.DESC, "ZXC"),//From override... No mapper for so use provided value
                new Sort.Order(Sort.Direction.ASC, "D321"),//From default... 123D gets mapped to D321
                new Sort.Order(Sort.Direction.ASC, "312") //From default... No mapper for so use provided value
            ));
    }

    @Test
    void toPaginatedList_withoutMapper_shouldUseDefaultMapper() {
        PaginationDto<String> paginationDto = createValidObject();

        PaginatedList<String> paginatedList = mock(PaginatedList.class);
        doReturn(paginatedList).when(paginationDto)
            .toPaginatedList(any(), any(), any(), any(), any());

        Function<Pageable, Page<String>> page = mock(Function.class);
        Function<String, String> dataMapper = mock(Function.class);
        List<String> sortOrder = List.of("defaultSortField");
        List<Sort.Direction> sortDirection = List.of(Sort.Direction.ASC);

        assertThat(paginationDto.toPaginatedList(page, dataMapper, sortOrder, sortDirection))
            .isEqualTo(paginatedList);

        verify(paginationDto).toPaginatedList(
            page,
            dataMapper,
            sortOrder,
            sortDirection,
            Map.of()
        );
    }

    @Test
    void toPaginatedList_withMapper_shouldCreateTheCorrectData() {
        PaginationDto<String> paginationDto = createValidObject();

        Map<String, String> sortByMapper = Map.of("defaultSortField", "defaultSortField234");

        Function<Pageable, Page<Integer>> pageFunction = mock(Function.class);
        Function<Integer, String> dataMapperFunction = integer -> integer.toString();

        List<String> sortOrder = List.of("defaultSortField");
        List<Sort.Direction> sortDirection = List.of(Sort.Direction.ASC);

        Pageable pageable = mock(Pageable.class);
        doReturn(pageable).when(paginationDto).toPagable(any(), any(), any());

        Page<Integer> page = mock(Page.class);
        when(page.getTotalElements()).thenReturn(100L);
        doReturn(page).when(pageFunction).apply(any());
        when(page.get()).thenReturn(List.of(123, 456, 789, 101_112).stream());


        PaginatedList<String> result = paginationDto.toPaginatedList(pageFunction, dataMapperFunction, sortOrder, sortDirection, sortByMapper);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalItems()).isEqualTo(100);
        assertThat(result.getTotalPages()).isEqualTo(10);
        assertThat(result.getData()).hasSize(4)
            .containsExactly("123", "456", "789", "101112");

        verify(paginationDto)
            .toPagable(
                sortOrder,
                sortDirection,
                sortByMapper
            );
        verify(pageFunction).apply(pageable);
    }
}
