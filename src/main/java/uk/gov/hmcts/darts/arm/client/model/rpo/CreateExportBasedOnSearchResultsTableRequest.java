package uk.gov.hmcts.darts.arm.client.model.rpo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
@ToString()
public class CreateExportBasedOnSearchResultsTableRequest {

    private String core;
    private String formFields;
    @NonNull
    private String searchId;
    @NonNull
    private Integer searchitemsCount;
    @NonNull
    private List<HeaderColumn> headerColumns;
    @NonNull
    private String productionName;
    @NonNull
    private String storageAccountId;

    @Data
    @Builder
    @Jacksonized
    public static class HeaderColumn {
        private String masterIndexField;
        private String displayName;
        private String propertyName;
        private String propertyType;
        private Boolean isMasked;
    }

}
