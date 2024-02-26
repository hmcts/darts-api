package uk.gov.hmcts.darts.log.util;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
public class LogJobReport {
    private Map<JobStatusType, Integer> mapOfResults = new HashMap<JobStatusType, Integer>();

    public static final int ZERO_ENTRIES = 0;

    private int total = ZERO_ENTRIES;
    private SourceType source;

    private String jobTitle;

    private final static String REPORT_MESSAGE =
        "{}: source={}, job_status={}}, total={}, processed={}, partially_processed={}, failed={}, ignored={}";

    public LogJobReport(String jobTitle, int total, SourceType source) {
        this.total = total;
        this.source = source;
        this.jobTitle = jobTitle;
    }

    public void registerResult(JobStatusType status) {
        if (mapOfResults.containsKey(status)) {
            mapOfResults.put(status, 0);
        }

        mapOfResults.put(status, mapOfResults.get(status) + 1);
    }

    public boolean haveAllProcessed() {
        return getProcessed() == total;
    }

    public int getAggregatedResultCount() {
        int processed = 0;
        for (JobStatusType status : mapOfResults.keySet()) {
            if (status != JobStatusType.NEW) {
                processed = processed + mapOfResults.get(status);
            }
        }

        return processed;
    }

    private int getCountForJobStatus(JobStatusType status) {
        if (mapOfResults.containsKey(status)) {
            return mapOfResults.get(status);
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
        return REPORT_MESSAGE.formatted(jobTitle, source.toString(), status,
                                                   getProcessed(), getPartiallyProcessed(), getFailed(), getIgnored());
    }
}