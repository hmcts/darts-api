package uk.gov.hmcts.darts.retention;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetentionFunctionalTest extends FunctionalTest {

    public static final String CASE_RETENTION_URI = "/retentions";
    public static final int OK = 200;

    @AfterEach
    public void teardown() {
        clean();
    }

    @Test
    void testGetCaseRetention() {
        String caseId = createCaseRetentions();

        Response response = buildRequestWithExternalAuth()
              .contentType(ContentType.JSON)
              .when()
              .baseUri(getUri(CASE_RETENTION_URI))
              .param("case_id", caseId)
              .get()
              .then()
              .assertThat()
              .statusCode(OK)
              .extract().response();

        assertEquals(200, response.getStatusCode());

        String retentionPolicyApplied = response.jsonPath().getString("[0].retention_policy_applied");
        assertEquals("Legacy Standard", retentionPolicyApplied);
    }
}
