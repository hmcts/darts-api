package uk.gov.hmcts.darts.audit;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

class AuditSearchFunctionalTest extends FunctionalTest {
    public static final String SEARCH_ENDPOINT = "/audit/search";

    @Test
    void success() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .queryParam("from_date", "2023-09-11T08:13:09.688537759Z")
            .queryParam("to_date", "2023-09-11T19:13:09.688537759Z")
            .when()
            .baseUri(getUri(SEARCH_ENDPOINT))
            .redirects().follow(false)
            .get().then().extract().response();

        response.asPrettyString();
    }
}
