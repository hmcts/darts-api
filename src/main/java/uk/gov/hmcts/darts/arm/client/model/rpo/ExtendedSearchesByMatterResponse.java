package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExtendedSearchesByMatterResponse extends BaseRpoResponse {

    private List<SearchDetail> searches;

    @Data
    @NoArgsConstructor
    public static class SearchDetail {
        private Search search;
    }

    @Data
    @NoArgsConstructor
    public static class Search {

        private Integer totalCount;

    }


}
