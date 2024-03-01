package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;

class DailyListLoggerServiceImplTest {
    private static LogCaptor logCaptor;

    private DailyListLoggerServiceImpl dailyListLoggerService;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(DailyListLoggerServiceImpl.class);
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @BeforeEach
    void setUp() {
        dailyListLoggerService = new DailyListLoggerServiceImpl();
    }

    @Test
    void logsDailyListReportCorrectly() {
        DailyListLogJobReport report = new DailyListLogJobReport(1, SourceType.XHB);
        report.registerResult(JobStatusType.PROCESSED);

        logCaptor.setLogLevelToInfo();
        dailyListLoggerService.logJobReport(report);

        Assertions.assertTrue(logCaptor.getInfoLogs().contains(report.toString()));
    }

    @Test
    void logsDailyListReportInvalid() {
        DailyListLogJobReport report = new DailyListLogJobReport(1, SourceType.XHB);

        dailyListLoggerService.logJobReport(report);

        Assertions.assertTrue(logCaptor.getErrorLogs().contains(report.toString()));
    }
}