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

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public class MasterIndexField {

        private boolean isRecordClass;
        private boolean isPublisher;
        private boolean isRegion;
        private List<DropDownOption> dropDownOptions;
        private String watermark;
        private boolean defaultVisible;
        private boolean defaultRequireField;
        private boolean hasHistory;
        @JsonProperty("masterIndexFieldID")
        private String masterIndexFieldId;
        private String propertyName;
        private String propertyType;
        private String jsonPropertyType;
        private String displayName;
        private String inputType;
        private int order;
        private boolean isSearchable;
        private boolean isRetrievable;
        private boolean isSortable;
        private boolean isFilterable;
        private String insertExtension;
        private boolean highlight;
        private boolean isSimpleSearch;
        private boolean isAllFields;
        private boolean isAdvancedSearch;
        private boolean isLiteSearch;
        private boolean isSystem;
        private boolean isMandatory;
        private boolean isMandatoryForIndexing;
        private boolean isFromBlob;
        private boolean isFromParser;
        private boolean isDbSearch;
        @JsonProperty("onlyForDBSearch")
        private boolean onlyForDbSearch;
        private boolean isCriteriaLogic;
        private boolean isContent;
        private String parentId;
        private int type;
        private boolean previewHighlight;
        private boolean isReadOnly;
        @JsonProperty("isUsedForRM")
        private boolean isUsedForRm;
        private boolean isGlobal;
        private boolean isMasked;
        private String classification;
        private boolean required;
        private boolean isNew;
        private String acceptedValues;
        private String oldAcceptedValues;
        private boolean allowSubmission;
        private boolean enableHistory;
        private boolean isEditable;
        private List<Object> acceptedValuesList;
        private boolean dataScopeEntitlementNotApplied;
        private List<Object> oldAcceptedValuesList;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public class DropDownOption {
        private String masterIndexFieldSelectOptionsID;
        private String masterIndexFieldId;
        private String label;
        private String value;
        private boolean isSystem;

    }

    private List<MasterIndexField> masterIndexFields;
}


