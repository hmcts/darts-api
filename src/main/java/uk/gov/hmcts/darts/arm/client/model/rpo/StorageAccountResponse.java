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
    public static class Index {
        private IndexDetails index;
        private boolean isMultiStream;
        private List<Object> children;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndexDetails {
        @JsonProperty("indexID")
        private String indexId;
        private boolean isGroup;
        private String name;
        private String displayName;
        @JsonProperty("userIndexID")
        private String userIndexId;
        @JsonProperty("userID")
        private String userId;
        @JsonProperty("azureSearchAccountID")
        private String azureSearchAccountId;
        @JsonProperty("indexStatusID")
        private int indexStatusId;
        private boolean notified;
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
        private double totalTime;
        private int blobCount;
        private int blobsProcessed;
        private int indexDiscoveryItemsCount;
        private int indexDiscoveryItemsProcessed;
        private Integer exceptionsCount;
        private int indexBlobExceptionsCount;
        private Integer indexDiscoveryItemExceptionsCount;
        private int indexBatchJobPartitionsCount;
        private Integer indexExceptionBatchPartitionsCount;
        private int indexUpdateBatchPartitionsCount;
        private Integer indexUpdateExceptionBatchPartitionsCount;
        private Integer indexBatchLastJobPartitionsCount;
        private int indexBlobPartitionsCount;
        private Integer indexBlobJobExceptionPartitionsCount;
        private Integer indexBlobLastJobExceptionPartitionsCount;
        private int indexDiscoveryItemPartitionsCount;
        private Integer indexDiscoveryItemExceptionPartitionsCount;
        private Integer indexJobExceptionPartitionsCount;
        private Integer indexLastJobExceptionPartitionsCount;
        private int tablePartitionSize;
        private String updateDate;
        @JsonProperty("lastJobID")
        private int lastJobId;
        @JsonProperty("jobID")
        private int jobId;
        @JsonProperty("indexBlobLastJobID")
        private Integer indexBlobLastJobId;
        @JsonProperty("indexBlobJobID")
        private int indexBlobJobId;
        private boolean isContinous;
        private boolean isPrimary;
        @JsonProperty("isUsedForRM")
        private boolean isUsedForRm;
        private int continousIndexBlobPartitionsCount;
        private int requestSizeLimit;
        private boolean skipContentOverLimit;
        private boolean skipContentIfParserError;
        private int fileSizeLimitToTikaParser;
        private boolean sortByResultField;
        private String blobContainer;
        private int continuousIndexBatchSize;
        private int continousTablePartitionSize;
        private boolean isDeleted;
        private int mainQueueProcessPriority;
        private int secondaryQueueProcessPriority;
        private int buildBatchesInQueue;
        private int buildBatchesProcessed;
        private String buildContinuationToken;
        private String buildExceptionContinuationToken;
        private String updateContinuationToken;
        private String updateExceptionContinuationToken;
        private boolean countOnly;
        private String errorCodes;
        private Integer esIndexRolloverSize;
        private int esIndexRolloverSizePerShard;
        private int esIndexNoReplicas;
        private int esIndexNoShards;
        private boolean isDiscoveryCancelled;
        private Integer indexContinuousLastSavedBlobExceptionPartitionsCount;
        private int continuousExceptionsInProgress;
        private boolean preparingContinuousErrorBatches;
        private boolean schemaUpdated;
        private boolean discoveryItemsLock;
        private boolean blobExceptionsStreamUpdateNeeded;
        @JsonProperty("streamIDsToProcess")
        private String streamIdsToProcess;
        private int indexUpdateBlobPartitionsCount;
        private int indexUpdateExceptionPartitionsCount;
        private int indexUpdateExceptionsCount;
        private int updateBlobCount;
        private int updateBlobsProcessed;
        private Integer indexLastSavedUpdateExceptionPartitionsCount;
        private int updateExceptionsInProgress;
        private boolean preparingUpdateErrorBatches;
        private boolean poisonHandlingFailed;
    }

}
