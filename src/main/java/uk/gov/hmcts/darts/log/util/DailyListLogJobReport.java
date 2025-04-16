package uk.gov.hmcts.darts.log.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@EqualsAndHashCode
@SuppressWarnings("PMD.UseEnumCollections")//TODO - Refactor to use EnumMap
public class DailyListLogJobReport {
    private Map<JobStatusType, Integer> mapOfJobResultCount = new HashMap<>();

    public static final int ZERO_ENTRIES = 0;

    public static final String JOB_TITLE = "Daily list job";

    private int totalDailyListRegisteredJobStatus;

    private SourceType source;

    private String jobTitle = JOB_TITLE;

    private static final String REPORT_MESSAGE =
        "%1$s: source=%2$s, job_status=%3$s, total=%4$s, processed=%5$s, partially_processed=%6$s, failed=%7$s, ignored=%8$s";

    public DailyListLogJobReport(int totalDailyListRegisteredJobStatus, SourceType source) {
        this.totalDailyListRegisteredJobStatus = totalDailyListRegisteredJobStatus;
        this.source = source;
    }

    public void registerResult(JobStatusType status) {
        if (status != null && !mapOfJobResultCount.containsKey(status)) {
            mapOfJobResultCount.put(status, 1);
        } else if (status != null) {
            mapOfJobResultCount.put(status, mapOfJobResultCount.get(status) + 1);
        }
    }

    public boolean haveAllExpectedResults() {
        return getAggregatedResultCount() >= totalDailyListRegisteredJobStatus;
    }

    public int getAggregatedResultCount() {
        int processed = 0;
        for (JobStatusType status : mapOfJobResultCount.keySet()) {
            if (status != JobStatusType.NEW) {
                processed = processed + mapOfJobResultCount.get(status);
            }
        }

        return processed;
    }

    private int getCountForJobStatus(JobStatusType status) {
        if (mapOfJobResultCount.containsKey(status)) {
            return mapOfJobResultCount.get(status);
        }

        return ZERO_ENTRIES;
    }

    public int getFailedCount() {
        return getCountForJobStatus(JobStatusType.FAILED);
    }

    public int getIgnoredCount() {
        return getCountForJobStatus(JobStatusType.IGNORED);
    }

    public int getProcessedCount() {
        return getCountForJobStatus(JobStatusType.PROCESSED);
    }

    public int getPartiallyProcessedCount() {
        return getCountForJobStatus(JobStatusType.PARTIALLY_PROCESSED);
    }

    public void registerFailed() {
        registerResult(JobStatusType.FAILED);
    }

    @Override
    public String toString() {
        String status = haveAllExpectedResults() ? "COMPLETED" : "FAILED";

        return getReportString(jobTitle, source, status, totalDailyListRegisteredJobStatus,
                               getProcessedCount(), getPartiallyProcessedCount(), getFailedCount(), getIgnoredCount());
    }

    public static String getReportString(String title, SourceType source, String status,
                                         int total, int processed, int partiallyProcessed, int failed, int ignored) {
        return REPORT_MESSAGE.formatted(title, source.toString(), status,
                                        total, processed, partiallyProcessed, failed, ignored);
    }
}