package uk.gov.hmcts.darts.util.pagination;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginatedListTest {

    private PaginatedList<String> createValidObject() {
        return createValidObject(new PaginatedList<>());
    }

    private PaginatedList<String> createValidObject(PaginatedList<String> paginatedList) {
        paginatedList.setCurrentPage(1);
        paginatedList.setTotalPages(1);
        paginatedList.setTotalItems(1);
        return paginatedList;
    }


    @Test
    void setTotalItems_TypicalMultiplePagesOnLimit() {
        PaginatedList<String> list = createValidObject();
        list.setTotalItems(100, 25);
        assertThat(list.getTotalItems()).isEqualTo(100);
        assertThat(list.getTotalPages()).isEqualTo(4);
    }

    @Test
    void setTotalItems_TypicalMultiplePagesBelowLimit() {
        PaginatedList<String> list = createValidObject();
        list.setTotalItems(100, 24);
        assertThat(list.getTotalItems()).isEqualTo(100);
        assertThat(list.getTotalPages()).isEqualTo(5);
    }

    @Test
    void setTotalItems_TypicalSinglePageOnLimit() {
        PaginatedList<String> list = createValidObject();
        list.setTotalItems(25, 25);
        assertThat(list.getTotalItems()).isEqualTo(25);
        assertThat(list.getTotalPages()).isEqualTo(1);
    }

    @Test
    void setTotalItems_TypicalSingleBelowLimit() {
        PaginatedList<String> list = createValidObject();
        list.setTotalItems(20, 25);
        assertThat(list.getTotalItems()).isEqualTo(20);
        assertThat(list.getTotalPages()).isEqualTo(1);
    }

    @Test
    void setTotalItems_ZeroTotalItems() {
        PaginatedList<String> list = createValidObject();
        list.setTotalItems(0, 25);
        assertThat(list.getTotalItems()).isEqualTo(0);
        assertThat(list.getTotalPages()).isEqualTo(0);

    }

    @Test
    void isEmpty_TrueEmptyList() {
        PaginatedList<String> list = createValidObject();
        list.setData(List.of());
        assertThat(list.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_TrueNullList() {
        PaginatedList<String> list = createValidObject();
        list.setData(null);
        assertThat(list.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_False() {
        PaginatedList<String> list = createValidObject();
        list.setData(List.of("Any"));
        assertThat(list.isEmpty()).isFalse();
    }


    @Nested
    class AsClassTests {
        @SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive This is a support class not a test class
        class TestClass1 extends PaginatedList<String> {
        }

        @SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive This is a support class not a test class
        class TestClass2 extends PaginatedList<String> {
        }

        @Test
        void asClass_paginatedListIsSameAsClass_shouldReturnCastedInstance() {
            PaginatedList<String> list = createValidObject(new TestClass1());
            TestClass1 testClass = list.asClass(TestClass1.class);
            assertThat(testClass).isNotNull();
        }

        @Test
        void asClass_paginatedListIsNotSameAsClass_shouldReturnNull() {
            PaginatedList<String> list = createValidObject(new TestClass1());
            DartsApiException exception = assertThrows(DartsApiException.class,
                                                       () -> list.asClass(TestClass2.class));
            assertThat(exception.getError()).isEqualTo(CommonApiError.INTERNAL_SERVER_ERROR);
            assertThat(exception.getMessage())
                .isEqualTo("Internal server error. Invalid class type provided for conversion: "
                               + "uk.gov.hmcts.darts.util.pagination.PaginatedListTest$AsClassTests$TestClass2. "
                               + "Expected: "
                               + "uk.gov.hmcts.darts.util.pagination.PaginatedListTest$AsClassTests$TestClass1."
                );


        }
    }
}
