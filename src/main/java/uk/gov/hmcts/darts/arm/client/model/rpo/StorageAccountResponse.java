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
public class StorageAccountResponse extends AbstractMatterResponse {

    private List<Index> indexes;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode
    public static class Index {
        private IndexDetails index;
        private Boolean isMultiStream;
        private List<Object> children;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndexDetails {
        @JsonProperty("indexID")
        private String indexId;
        private Boolean isGroup;
        private String name;
        private String displayName;
        @JsonProperty("userIndexID")
        private String userIndexId;
        @JsonProperty("userID")
        private String userId;
        @JsonProperty("azureSearchAccountID")
        private String azureSearchAccountId;
        @JsonProperty("indexStatusID")
        private Integer indexStatusId;
        private Boolean notified;
        private String startDate;
        private String endDate;
        private String discoveryStartDate;
        private String discoveryEndDate;
        private String buildStartDate;
        private String buildEndDate;
        private String resumeStartDate;
        private String resumeEndDate;
        private String stoppingStartDate;
        private String stoppingEndDate;
        private String createdDate;
        private Double totalTime;
        private Integer blobCount;
        private Integer blobsProcessed;
        private Integer indexDiscoveryItemsCount;
        private Integer indexDiscoveryItemsProcessed;
        private Integer exceptionsCount;
        private Integer indexBlobExceptionsCount;
        private Integer indexDiscoveryItemExceptionsCount;
        private Integer indexBatchJobPartitionsCount;
        private Integer indexExceptionBatchPartitionsCount;
        private Integer indexUpdateBatchPartitionsCount;
        private Integer indexUpdateExceptionBatchPartitionsCount;
        private Integer indexBatchLastJobPartitionsCount;
        private Integer indexBlobPartitionsCount;
        private Integer indexBlobJobExceptionPartitionsCount;
        private Integer indexBlobLastJobExceptionPartitionsCount;
        private Integer indexDiscoveryItemPartitionsCount;
        private Integer indexDiscoveryItemExceptionPartitionsCount;
        private Integer indexJobExceptionPartitionsCount;
        private Integer indexLastJobExceptionPartitionsCount;
        private Integer tablePartitionSize;
        private String updateDate;
        @JsonProperty("lastJobID")
        private Integer lastJobId;
        @JsonProperty("jobID")
        private Integer jobId;
        @JsonProperty("indexBlobLastJobID")
        private Integer indexBlobLastJobId;
        @JsonProperty("indexBlobJobID")
        private Integer indexBlobJobId;
        private Boolean isContinous;
        private Boolean isPrimary;
        @JsonProperty("isUsedForRM")
        private Boolean isUsedForRm;
        private Integer continousIndexBlobPartitionsCount;
        private Integer requestSizeLimit;
        private Boolean skipContentOverLimit;
        private Boolean skipContentIfParserError;
        private Integer fileSizeLimitToTikaParser;
        private Boolean sortByResultField;
        private String blobContainer;
        private Integer continuousIndexBatchSize;
        private Integer continousTablePartitionSize;
        private Boolean isDeleted;
        private Integer mainQueueProcessPriority;
        private Integer secondaryQueueProcessPriority;
        private Integer buildBatchesInQueue;
        private Integer buildBatchesProcessed;
        private String buildContinuationToken;
        private String buildExceptionContinuationToken;
        private String updateContinuationToken;
        private String updateExceptionContinuationToken;
        private Boolean countOnly;
        private String errorCodes;
        private Integer esIndexRolloverSize;
        private Integer esIndexRolloverSizePerShard;
        private Integer esIndexNoReplicas;
        private Integer esIndexNoShards;
        private Boolean isDiscoveryCancelled;
        private Integer indexContinuousLastSavedBlobExceptionPartitionsCount;
        private Integer continuousExceptionsInProgress;
        private Boolean preparingContinuousErrorBatches;
        private Boolean schemaUpdated;
        private Boolean discoveryItemsLock;
        private Boolean blobExceptionsStreamUpdateNeeded;
        @JsonProperty("streamIDsToProcess")
        private String streamIdsToProcess;
        private Integer indexUpdateBlobPartitionsCount;
        private Integer indexUpdateExceptionPartitionsCount;
        private Integer indexUpdateExceptionsCount;
        private Integer updateBlobCount;
        private Integer updateBlobsProcessed;
        private Integer indexLastSavedUpdateExceptionPartitionsCount;
        private Integer updateExceptionsInProgress;
        private Boolean preparingUpdateErrorBatches;
        private Boolean poisonHandlingFailed;
    }

}
