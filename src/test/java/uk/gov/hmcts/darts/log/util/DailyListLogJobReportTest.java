package uk.gov.hmcts.darts.log.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;

class DailyListLogJobReportTest {

    private static final String REGEX_FOR_REPORT = ".*: source=.*, job_status=.*. total=.*, processed=.*, partially_processed=.*, failed=.*, ignored=.*";

    @Test
    void testBasicLoggingDetails() {
        SourceType source = SourceType.CPP;
        int totalNumberOfRequests = 20;
        String status = "status";

        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(totalNumberOfRequests, SourceType.CPP);

        Assertions.assertEquals(DailyListLogJobReport.JOB_TITLE, dailyListLogJobReport.getJobTitle());
        Assertions.assertTrue(dailyListLogJobReport.toString().matches(REGEX_FOR_REPORT));
        Assertions.assertEquals(totalNumberOfRequests, dailyListLogJobReport.getTotalDailyListRegisteredJobStatus());
        Assertions.assertEquals(SourceType.CPP, dailyListLogJobReport.getSource());

        Assertions.assertTrue(DailyListLogJobReport
            .getReportString(DailyListLogJobReport.JOB_TITLE, source, status, 10,0,0, 0, 0).matches(REGEX_FOR_REPORT));

    }

    @Test
    void testBasicLoggingAndProcessingFailure() {
        int totalNumberOfRequests = 10;
        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(totalNumberOfRequests, SourceType.CPP);

        Assertions.assertEquals(DailyListLogJobReport.JOB_TITLE, dailyListLogJobReport.getJobTitle());
        Assertions.assertTrue(dailyListLogJobReport.toString().matches(REGEX_FOR_REPORT));
        Assertions.assertEquals(totalNumberOfRequests, dailyListLogJobReport.getTotalDailyListRegisteredJobStatus());
        Assertions.assertEquals(SourceType.CPP, dailyListLogJobReport.getSource());

        dailyListLogJobReport.registerResult(JobStatusType.IGNORED);
        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.IGNORED);
        dailyListLogJobReport.registerResult(JobStatusType.PARTIALLY_PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.FAILED);
        dailyListLogJobReport.registerResult(JobStatusType.IGNORED);
        dailyListLogJobReport.registerResult(JobStatusType.FAILED);
        dailyListLogJobReport.registerResult(JobStatusType.IGNORED);
        dailyListLogJobReport.registerResult(JobStatusType.IGNORED);

        SourceType source = SourceType.CPP;
        String status = "COMPLETED";
        Assertions.assertEquals(DailyListLogJobReport
                                  .getReportString(DailyListLogJobReport.JOB_TITLE,
                                                   source, status, totalNumberOfRequests,2,1, 2, 5), dailyListLogJobReport.toString());
    }

    @Test
    void testBasicLoggingAndProcessingFailureNotProcessedAll() {
        int totalNumberOfRequests = 10;

        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(totalNumberOfRequests, SourceType.CPP);

        Assertions.assertEquals(DailyListLogJobReport.JOB_TITLE, dailyListLogJobReport.getJobTitle());
        Assertions.assertTrue(dailyListLogJobReport.toString().matches(REGEX_FOR_REPORT));
        Assertions.assertEquals(totalNumberOfRequests, dailyListLogJobReport.getTotalDailyListRegisteredJobStatus());
        Assertions.assertEquals(SourceType.CPP, dailyListLogJobReport.getSource());

        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.NEW);

        SourceType source = SourceType.CPP;
        String status = "FAILED";

        Assertions.assertEquals(2, dailyListLogJobReport.getAggregatedResultCount());
        Assertions.assertFalse(dailyListLogJobReport.haveAllExpectedResults());
        Assertions.assertEquals(DailyListLogJobReport
                                    .getReportString(DailyListLogJobReport.JOB_TITLE,
                                                     source, status, totalNumberOfRequests,2,0, 0, 0), dailyListLogJobReport.toString());
    }

    @Test
    void testBasicLoggingAndReportingMoreResultsThanExpected() {
        int totalNumberOfRequests = 5;

        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(totalNumberOfRequests, SourceType.CPP);

        Assertions.assertEquals(DailyListLogJobReport.JOB_TITLE, dailyListLogJobReport.getJobTitle());
        Assertions.assertTrue(dailyListLogJobReport.toString().matches(REGEX_FOR_REPORT));
        Assertions.assertEquals(totalNumberOfRequests, dailyListLogJobReport.getTotalDailyListRegisteredJobStatus());
        Assertions.assertEquals(SourceType.CPP, dailyListLogJobReport.getSource());

        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.IGNORED);
        dailyListLogJobReport.registerResult(JobStatusType.FAILED);
        dailyListLogJobReport.registerResult(JobStatusType.PARTIALLY_PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.PARTIALLY_PROCESSED);

        SourceType source = SourceType.CPP;
        String status = "COMPLETED";

        Assertions.assertEquals(6, dailyListLogJobReport.getAggregatedResultCount());
        Assertions.assertEquals(DailyListLogJobReport
                                    .getReportString(DailyListLogJobReport.JOB_TITLE,
                                                     source, status, totalNumberOfRequests,2,2, 1, 1), dailyListLogJobReport.toString());
    }

    @Test
    void testBasicLoggingAndProcessingSuccess() {
        int totalNumberOfRequests = 2;

        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(totalNumberOfRequests, SourceType.CPP);

        Assertions.assertEquals(DailyListLogJobReport.JOB_TITLE, dailyListLogJobReport.getJobTitle());
        Assertions.assertTrue(dailyListLogJobReport.toString().matches(REGEX_FOR_REPORT));
        Assertions.assertEquals(totalNumberOfRequests, dailyListLogJobReport.getTotalDailyListRegisteredJobStatus());
        Assertions.assertEquals(SourceType.CPP, dailyListLogJobReport.getSource());

        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);

        SourceType source = SourceType.CPP;
        String status = "COMPLETED";

        Assertions.assertTrue(dailyListLogJobReport.haveAllExpectedResults());
        Assertions.assertEquals(DailyListLogJobReport
                                    .getReportString(DailyListLogJobReport.JOB_TITLE, source,
                                                     status, totalNumberOfRequests,2,0, 0, 0), dailyListLogJobReport.toString());
    }

    @Test
    void testGetLogging() {
        String title = "this is the title";
        SourceType source = SourceType.CPP;
        String status = "status";

        Assertions.assertTrue(DailyListLogJobReport
                                    .getReportString(title, source, status, 10,0,0, 0, 0).matches(REGEX_FOR_REPORT));
    }

    @Test
    void testDoesNotConsiderNullStatus() {
        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(2, SourceType.CPP);
        dailyListLogJobReport.registerResult(JobStatusType.PROCESSED);
        dailyListLogJobReport.registerResult(null);

        DailyListLogJobReport reportAssertion
            = new DailyListLogJobReport(2, SourceType.CPP);
        reportAssertion.registerResult(JobStatusType.PROCESSED);

        Assertions.assertEquals(reportAssertion, dailyListLogJobReport);
    }

    @Test
    void testExpectNothingToBeProcessed() {
        DailyListLogJobReport dailyListLogJobReport
            = new DailyListLogJobReport(0, SourceType.CPP);
        String status = "COMPLETED";
        String title = DailyListLogJobReport.JOB_TITLE;
        SourceType source = SourceType.CPP;

        Assertions.assertEquals(dailyListLogJobReport.toString(), DailyListLogJobReport
                                  .getReportString(title, source, status, 0,0,0, 0, 0));
    }
}