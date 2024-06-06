package uk.gov.hmcts.darts.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.common.model.HiddenReason;
import uk.gov.hmcts.darts.testutil.TestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;

class HiddenReasonFunctionalTest extends FunctionalTest {

    private static final String ADMIN_HIDDEN_REASONS_BASE_PATH = "/admin/hidden-reasons";

    @Test
    void shouldGetAllHiddenReasons() throws JsonProcessingException {
        // When
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .get(getUri(ADMIN_HIDDEN_REASONS_BASE_PATH))
            .thenReturn();

        // Then
        assertEquals(200, response.getStatusCode());

        List<HiddenReason> hiddenReasons = TestUtils.createObjectMapper()
            .readValue(response.asString(), new TypeReference<>() {});
        assertEquals(4, hiddenReasons.size());
    }

}
