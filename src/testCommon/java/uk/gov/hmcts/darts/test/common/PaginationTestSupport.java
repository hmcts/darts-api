package uk.gov.hmcts.darts.test.common;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Component
public class PaginationTestSupport {
    @Autowired
    protected ObjectMapper objectMapper;

    public <T> PaginatedList<T> assertPaginationDetails(MvcResult mvcResult, Class<T> clazz,
                                                        int currentPage, int pageSize, int totalPages, int totalItems) {
        PaginatedList<T> paginatedList = getPaginatedList(mvcResult, clazz);
        return assertPaginationDetails(paginatedList, currentPage, pageSize, totalPages, totalItems);
    }

    public <T> PaginatedList<T> assertPaginationDetails(PaginatedList<T> paginatedList, int currentPage, int pageSize, int totalPages, int totalItems) {
        assertThat(paginatedList.getCurrentPage())
            .describedAs("Current page should be %s", currentPage)
            .isEqualTo(currentPage);
        assertThat(paginatedList.getTotalPages())
            .describedAs("Total pages should be %s", totalPages)
            .isEqualTo(totalPages);
        assertThat(paginatedList.getTotalItems())
            .describedAs("Total items should be %s", totalItems)
            .isEqualTo(totalItems);
        assertThat(paginatedList.getPageSize())
            .describedAs("Page size should be %s", pageSize)
            .isEqualTo(pageSize);
        return paginatedList;
    }

    @SneakyThrows
    public <T> PaginatedList<T> getPaginatedList(MvcResult mvcResult, Class<T> clazz) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(PaginatedList.class, clazz);
        PaginatedList<T> paginatedList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), javaType);
        return paginatedList;
    }
}
