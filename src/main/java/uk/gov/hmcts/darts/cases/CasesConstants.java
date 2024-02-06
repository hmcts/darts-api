package uk.gov.hmcts.darts.cases;

public class CasesConstants {

    public static class GetCasesParams {

        public static final String COURTHOUSE = "courthouse";
        public static final String COURTROOM = "courtroom";
        public static final String DATE = "date";
    }

    public static class GetSearchCasesParams {

        public static final String ENDPOINT_URL = "/cases/search";
        public static final String CASE_NUMBER = "case_number";
        public static final String COURTHOUSE = "courthouse";
        public static final String COURTROOM = "courtroom";
        public static final String JUDGE_NAME = "judge_name";
        public static final String DEFENDANT_NAME = "defendant_name";
        public static final String DATE_FROM = "date_from";
        public static final String DATE_TO = "date_to";
        public static final String EVENT_TEXT_CONTAINS = "event_text_contains";

    }
}
