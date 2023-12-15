package uk.gov.hmcts.darts.usermanagement;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityGroupFunctionalTest extends FunctionalTest {

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void shouldCreateSecurityGroup() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/security-groups"))
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "name": "ACME",
                      "display_name": "ACME Transcription Services",
                      "description": "A temporary group created by functional test"
                    }
                      """)
            .post()
            .thenReturn();

        assertEquals(201, response.statusCode());

        JSONAssert.assertEquals(
            """
                {
                  "id": "",
                  "name": "ACME",
                  "display_name": "ACME Transcription Services",
                  "description": "A temporary group created by functional test",
                  "display_state": true,
                  "global_access": false,
                  "role_id": 4
                }
                """,
            response.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("\\d+"))
            )
        );
    }

}
