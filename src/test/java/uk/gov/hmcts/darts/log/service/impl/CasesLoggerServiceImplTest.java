package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;

import static org.assertj.core.api.Assertions.assertThat;

class CasesLoggerServiceImplTest {

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";

    private static LogCaptor logCaptor;

    private CasesLoggerService casesLoggerService;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(CasesLoggerServiceImpl.class);
        logCaptor.setLogLevelToInfo();
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
        casesLoggerService = new CasesLoggerServiceImpl();
    }

    @Test
    void logsGetCasesRequestCorrectly() {
        var request = someGetCasesRequest();

        casesLoggerService.casesRequestedByDarPc(request);

        var expectedLogEntry = String.format("getCases request received: courthouse=%s, courtroom=%s", SOME_COURTHOUSE, SOME_COURTROOM);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedLogEntry);
    }

    private GetCasesRequest someGetCasesRequest() {
        var getCasesRequest = new GetCasesRequest();
        getCasesRequest.setCourthouse(SOME_COURTHOUSE);
        getCasesRequest.setCourtroom(SOME_COURTROOM);
        return getCasesRequest;
    }
}