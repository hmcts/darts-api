package uk.gov.hmcts.darts.retention;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class RetentionPolicyTypeFunctionalTest extends FunctionalTest {

    public static final String ADMIN_RETENTION_POLICY_TYPES = "/admin/retention-policy-types";

    public static final String RETENTION_POLICY_TYPE_BY_ID = "/admin/retention-policy-types/1";

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void getAllRetentionPolicyTypes() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .when()
            .baseUri(getUri(ADMIN_RETENTION_POLICY_TYPES))
            .get()
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();

        JSONArray jsonResponseArray = new JSONArray(response.asString());
        assertFalse(jsonResponseArray.isEmpty());
    }

    @Test
    void getRetentionPolicyTypeById() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .when()
            .baseUri(getUri(RETENTION_POLICY_TYPE_BY_ID))
            .get()
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();

        assertEquals(200, response.getStatusCode());

        String retentionPolicyApplied = response.jsonPath().getString("id");
        assertEquals("1", retentionPolicyApplied);
    }

    @Test
    void createNewRetentionPolicy() {
        Response response = createRetentionPolicyType();

        JSONAssert.assertEquals(
            """
                {
                  "id": "",
                  "name": "Func test policy",
                  "display_name": "Func test policy",
                  "description": "FUNC-policy",
                  "fixed_policy_key": "999",
                  "duration": "1Y0M0D",
                  "policy_start_at": "2124-01-01T00:00:00Z"
                }
                """,
            response.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("\\d+"))
            )
        );
    }

    @Test
    void editRetentionPolicy() {
        // Given
        Response createRetentionPolicyResponse = createRetentionPolicyType()
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_CREATED)
            .extract()
            .response();

        final int retentionPolicyId = new JSONObject(createRetentionPolicyResponse.asString())
            .getInt("id");

        // When
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .when()
            .baseUri(getUri(ADMIN_RETENTION_POLICY_TYPES) + "/" + retentionPolicyId)
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "name": "Updated func test policy name"
                      }
                      """)
            .patch()
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();

        // Then
        JSONAssert.assertEquals(
            """
                {
                  "id": "",
                  "name": "Updated func test policy name",
                  "display_name": "Func test policy",
                  "description": "FUNC-policy",
                  "fixed_policy_key": "999",
                  "duration": "1Y0M0D",
                  "policy_start_at": "2124-01-01T00:00:00Z"
                }
                """,
            response.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("^" + retentionPolicyId + "$"))
            )
        );
    }

    private Response createRetentionPolicyType() {
        return buildRequestWithExternalGlobalAccessAuth()
            .when()
            .baseUri(getUri(ADMIN_RETENTION_POLICY_TYPES))
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "name": "Func test policy",
                        "display_name": "Func test policy",
                        "description": "FUNC-policy",
                        "fixed_policy_key": "999",
                        "duration": "1Y0M0D",
                        "policy_start_at": "2124-01-01T00:00:00Z"
                      }
                      """)
            .post()
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().response();
    }

}
