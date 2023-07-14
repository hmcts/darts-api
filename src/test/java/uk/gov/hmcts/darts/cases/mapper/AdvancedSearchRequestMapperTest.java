package uk.gov.hmcts.darts.cases.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;

import java.time.LocalDate;

class AdvancedSearchRequestMapperTest {

    @Test
    public void justCase() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setCaseNumber("Test");
        AdvancedSearchRequestMapper mapper = new AdvancedSearchRequestMapper();
        String result = mapper.mapToSQL(request);
        System.out.println("Result = " + result);
    }

    @Test
    public void HearingDateFrom() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setDateFrom(LocalDate.of(2023, 5, 6));
        AdvancedSearchRequestMapper mapper = new AdvancedSearchRequestMapper();
        String result = mapper.mapToSQL(request);
        System.out.println("Result = " + result);
    }

    @Test
    public void Judge() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setJudgeName("judgesTest");
        AdvancedSearchRequestMapper mapper = new AdvancedSearchRequestMapper();
        String result = mapper.mapToSQL(request);
        System.out.println("Result = " + result);
    }

    @Test
    public void keywords1() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setKeywords("keywordsTest");
        AdvancedSearchRequestMapper mapper = new AdvancedSearchRequestMapper();
        String result = mapper.mapToSQL(request);
        System.out.println("Result = " + result);
    }

    @Test
    public void keywords5() {
        GetCasesSearchRequest request = new GetCasesSearchRequest();
        request.setKeywords("keywordsTest1 keywordsTest2 keywordsTest3 keywordsTest4 keywordsTest5");
        AdvancedSearchRequestMapper mapper = new AdvancedSearchRequestMapper();
        String result = mapper.mapToSQL(request);
        System.out.println("Result = " + result);
    }

}
