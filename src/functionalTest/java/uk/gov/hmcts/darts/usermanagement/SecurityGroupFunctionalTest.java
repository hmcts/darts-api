package uk.gov.hmcts.darts.usermanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.naturalOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityGroupFunctionalTest extends FunctionalTest {

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void shouldCreateSecurityGroup() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
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
                  "security_role_id": 4
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
    void shouldGetSecurityGroups() throws JsonProcessingException {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
            .contentType(ContentType.JSON)
            .get()
            .thenReturn();

        assertEquals(200, response.getStatusCode());

        ObjectMapper objectMapper = new ObjectMapper();
        List<SecurityGroupWithIdAndRole> securityGroupWithIdAndRoles = objectMapper.readValue(response.asString(), new TypeReference<List<SecurityGroupWithIdAndRole>>(){});
        assertFalse(securityGroupWithIdAndRoles.isEmpty());
    }
}
