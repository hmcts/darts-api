package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CasesLoggerServiceImplTest {

    private static final String SOME_COURTHOUSE = "SOME-CoUrTHoUsE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "some-case-number";

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

        var expectedLogEntry = String.format("getCases request received: courthouse=%s, courtroom=%s",
                                             SOME_COURTHOUSE.toUpperCase(Locale.ROOT),
                                             SOME_COURTROOM.toUpperCase(Locale.ROOT));
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedLogEntry);
    }

    private GetCasesRequest someGetCasesRequest() {
        var getCasesRequest = new GetCasesRequest();
        getCasesRequest.setCourthouse(SOME_COURTHOUSE);
        getCasesRequest.setCourtroom(SOME_COURTROOM);
        return getCasesRequest;
    }

    @Test
    void testDefendantNameOverflow() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setDefendants(List.of("name ".repeat(121)));
        addCaseRequest.setCourthouse(SOME_COURTHOUSE);
        addCaseRequest.setCaseNumber(SOME_CASE_NUMBER);

        casesLoggerService.defendantNameOverflow(addCaseRequest);

        List<String> logs = logCaptor.getWarnLogs();
        String logEntry = String.format("Defendant name overflow: case_number=%s, courthouse=%s",
                                        SOME_CASE_NUMBER, SOME_COURTHOUSE.toUpperCase(Locale.ROOT));

        assertEquals(1, logs.size());
        assertEquals(logEntry, logs.getFirst());
    }

    @Test
    void testDefendantNotAdded() {
        var defendant = "U20240603-103622, U20240603-03622";
        casesLoggerService.defendantNotAdded(defendant, SOME_CASE_NUMBER);

        List<String> logs = logCaptor.getInfoLogs();
        assertEquals(1, logs.size());
        assertEquals(String.format("Defendant not added to case: defendant=%s, case_number=%s", defendant, SOME_CASE_NUMBER), logs.getFirst());
    }

    @Test
    void testCaseDeletedDueToExpiry() {
        var caseId = 123;
        casesLoggerService.caseDeletedDueToExpiry(caseId, SOME_CASE_NUMBER);

        List<String> logs = logCaptor.getInfoLogs();
        assertEquals(1, logs.size());
        assertEquals(String.format("Case expired: cas_id=%s, case_number=%s", caseId, SOME_CASE_NUMBER), logs.getFirst());
    }
}