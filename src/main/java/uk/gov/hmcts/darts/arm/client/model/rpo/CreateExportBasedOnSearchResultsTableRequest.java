package uk.gov.hmcts.darts.arm.client.model.rpo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
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
    @NonNull
    private Boolean onlyForCurrentUser;
    @NonNull
    private Integer exportType;

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
