package uk.gov.hmcts.darts.cases.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvancedSearchRequestMapperTest {

    @Test
    public void justCase() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setCaseNumber("Test");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case
            WHERE upper(court_case.case_number) like upper(:caseNumber)""";
        assertEquals(expected, result);
    }

    @Test
    public void HearingDateFrom() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setDateFrom(LocalDate.of(2023, 5, 6));
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, hearing
            WHERE hearing.hearing_date >= date(:dateFrom)
            AND hearing.cas_id = court_case.cas_id""";
        assertEquals(expected, result);
    }

    @Test
    public void HearingDateTo() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setDateTo(LocalDate.of(2023, 5, 6));
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, hearing
            WHERE hearing.hearing_date <= date(:dateTo)
            AND hearing.cas_id = court_case.cas_id""";
        assertEquals(expected, result);
    }

    @Test
    public void Judge() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setJudgeName("judgesTest");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, ( select cas_id,unnest(judge_list) as judge from hearing) as judges
            WHERE upper(judges.judge) like upper(:judge)
            AND judges.cas_id = court_case.cas_id""";
        assertEquals(expected, result);
    }

    @Test
    public void eventTestContains() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setEventTextContains("keywordsTest");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, hearing, hearing_event_ae, event
            WHERE upper(event.event_text) like upper(:keyword)
            AND hearing.cas_id = court_case.cas_id
            AND hearing_event_ae.hea_id = hearing.hea_id
            AND hearing_event_ae.eve_id = event.eve_id""";
        assertEquals(expected, result);
    }

    @Test
    public void courthouse() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setCourthouse("courthousetest");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, courthouse
            WHERE upper(courthouse.courthouse_name) like upper(:courthouse)
            AND courthouse.cth_id = court_case.cth_id""";
        assertEquals(expected, result);
    }

    @Test
    public void courtroom() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setCourtroom("courtroomtest");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, hearing, courtroom
            WHERE upper(courtroom.courtroom_name) like upper(:courtroom)
            AND hearing.cas_id = court_case.cas_id
            AND hearing.ctr_id = courtroom.ctr_id""";
        assertEquals(expected, result);
    }

    @Test
    public void defendant() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setDefendantName("defendantTest");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, ( select cas_id,unnest(defendant_list) as defendant from court_case) as defendants
            WHERE upper(defendants.defendant) like upper(:defendant)
            AND defendants.cas_id = court_case.cas_id""";
        assertEquals(expected, result);
    }

    @Test
    public void eventJudge() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setEventTextContains("eventTest");
        request.setJudgeName("judgeTest");
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, ( select cas_id,unnest(judge_list) as judge from hearing) as judges, hearing, hearing_event_ae, event
            WHERE upper(judges.judge) like upper(:judge)
            AND upper(event.event_text) like upper(:keyword)
            AND judges.cas_id = court_case.cas_id
            AND hearing.cas_id = court_case.cas_id
            AND hearing_event_ae.hea_id = hearing.hea_id
            AND hearing_event_ae.eve_id = event.eve_id""";
        assertEquals(expected, result);
    }

    @Test
    public void eventDate() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setEventTextContains("eventTest");
        request.setDateFrom(LocalDate.of(2023, 5, 6));
        String result = AdvancedSearchRequestMapper.mapToSQLQueryModel(request).toString();
        System.out.println("Result = " + result);
        String expected = """
            SELECT distinct(court_case.cas_id)
            FROM court_case, ( select cas_id,unnest(judge_list) as judge from hearing) as judges, hearing, hearing_event_ae, event
            WHERE upper(judges.judge) like upper(:judge)
            AND upper(event.event_text) like upper(:keyword)
            AND judges.cas_id = court_case.cas_id
            AND hearing.cas_id = court_case.cas_id
            AND hearing_event_ae.hea_id = hearing.hea_id
            AND hearing_event_ae.eve_id = event.eve_id""";
        assertEquals(expected, result);
    }


}
