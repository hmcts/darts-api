package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MasterIndexFieldByRecordClassSchemaResponse extends AbstractMatterResponse {

    private List<MasterIndexField> masterIndexFields;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class MasterIndexField {

        private Boolean isRecordClass;
        private Boolean isPublisher;
        private Boolean isRegion;
        private List<DropDownOption> dropDownOptions;
        private String watermark;
        private Boolean defaultVisible;
        private Boolean defaultRequireField;
        private Boolean hasHistory;
        @JsonProperty("masterIndexFieldID")
        private String masterIndexFieldId;
        private String propertyName;
        private String propertyType;
        private String jsonPropertyType;
        private String displayName;
        private String inputType;
        private Integer order;
        private Boolean isSearchable;
        private Boolean isRetrievable;
        private Boolean isSortable;
        private Boolean isFilterable;
        private String insertExtension;
        private Boolean highlight;
        private Boolean isSimpleSearch;
        private Boolean isAllFields;
        private Boolean isAdvancedSearch;
        private Boolean isLiteSearch;
        private Boolean isSystem;
        private Boolean isMandatory;
        private Boolean isMandatoryForIndexing;
        private Boolean isFromBlob;
        private Boolean isFromParser;
        private Boolean isDbSearch;
        @JsonProperty("onlyForDBSearch")
        private Boolean onlyForDbSearch;
        private Boolean isCriteriaLogic;
        private Boolean isContent;
        private String parentId;
        private Integer type;
        private Boolean previewHighlight;
        private Boolean isReadOnly;
        @JsonProperty("isUsedForRM")
        private Boolean isUsedForRm;
        private Boolean isGlobal;
        private Boolean isMasked;
        private String classification;
        private Boolean required;
        private Boolean isNew;
        private String acceptedValues;
        private String oldAcceptedValues;
        private Boolean allowSubmission;
        private Boolean enableHistory;
        private Boolean isEditable;
        private List<Object> acceptedValuesList;
        private Boolean dataScopeEntitlementNotApplied;
        private List<Object> oldAcceptedValuesList;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class DropDownOption {
        private String masterIndexFieldSelectOptionsID;
        private String masterIndexFieldId;
        private String label;
        private String value;
        private Boolean isSystem;

    }


}


