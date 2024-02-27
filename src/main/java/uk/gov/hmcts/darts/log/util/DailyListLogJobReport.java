package uk.gov.hmcts.darts.log.util;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
public class DailyListLogJobReport {
    private Map<JobStatusType, Integer> mapOfJobResultCount = new HashMap<JobStatusType, Integer>();

    public static final int ZERO_ENTRIES = 0;

    public static final String JOB_TITLE = "Daily list job";

    private int totalJobs;

    private SourceType source;

    private String jobTitle = JOB_TITLE;

    private static final String REPORT_MESSAGE =
        "%1$s: source=%2$s, job_status=%3$s, total=%4$s, processed=%5$s, partially_processed=%6$s, failed=%7$s, ignored=%8$s";

    public DailyListLogJobReport(int total, SourceType source) {
        this.totalJobs = total;
        this.source = source;
    }

    public void registerResult(JobStatusType status) {
        if (!mapOfJobResultCount.containsKey(status)) {
            mapOfJobResultCount.put(status, 0);
        }

        mapOfJobResultCount.put(status, mapOfJobResultCount.get(status) + 1);
    }

    public boolean haveAllProcessed() {
        return getProcessed() >= totalJobs;
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

    private boolean isAllProcessed() {
        return getFailed() > 0;
    }

    public int getFailed() {
        return getCountForJobStatus(JobStatusType.FAILED);
    }

    public int getIgnored() {
        return getCountForJobStatus(JobStatusType.IGNORED);
    }

    public int getProcessed() {
        return getCountForJobStatus(JobStatusType.PROCESSED);
    }

    public int getPartiallyProcessed() {
        return getCountForJobStatus(JobStatusType.PARTIALLY_PROCESSED);
    }

    public void registerFailed() {
        registerResult(JobStatusType.FAILED);
    }

    @Override
    public String toString() {
        String status = haveAllProcessed() ? "COMPLETED" : "FAILED";

        // gets the unprocessed entries and adds them to the ignored count
        int unprocessed = getUnprocessedJobEntries() + getIgnored();

        return getReportString(jobTitle, source, status, totalJobs,
                               getProcessed(), getPartiallyProcessed(), getFailed(), unprocessed);
    }

    private int getUnprocessedJobEntries() {
        int diff = getTotalJobs() - getAggregatedResultCount();
        return Math.max(diff, 0);
    }

    public static String getReportString(String title, SourceType source, String status,
                                         int total, int processed, int partiallyProcessed, int failed, int ignored) {
        return REPORT_MESSAGE.formatted(title, source.toString(), status,
                                        total, processed, partiallyProcessed, failed, ignored);
    }
}