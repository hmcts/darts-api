package uk.gov.hmcts.darts.authorisation.component.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_HEARING_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@ExtendWith(MockitoExtension.class)
class HearingIdControllerAuthorisationImplTest {

    @Mock
    private Authorisation authorisation;

    private final ObjectMapper mapper = new ObjectMapper();
    private Set<SecurityRoleEnum> roles;

    private ControllerAuthorisation controllerAuthorisation;

    @BeforeEach
    void setUp() {
        roles = Set.of(
            JUDGE,
            REQUESTER,
            APPROVER,
            TRANSCRIBER,
            LANGUAGE_SHOP_USER,
            RCJ_APPEALS
        );
        controllerAuthorisation = new HearingIdControllerAuthorisationImpl(authorisation);
    }

    @Test
    void getContextId() {
        assertEquals(HEARING_ID, controllerAuthorisation.getContextId());
    }

    @Test
    void checkAuthorisationRequestBody() throws JsonProcessingException {
        String body = """
            {
              "case_id": 1,
              "hearing_id": 2,
              "media_id": 3,
              "media_request_id": 4,
              "transcription_id": 5
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByHearingId(2, roles);
    }

    @Test
    void checkAuthorisationRequestBodyWhenHearingIdMissing() throws JsonProcessingException {
        String body = """
            {
              "case_id": 1,
              "media_id": 3,
              "media_request_id": 4,
              "transcription_id": 5
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByHearingId(0, roles);
    }

    @Test
    void checkAuthorisationPathParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cases/1");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("hearing_id", "2")
        );

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByHearingId(2, roles);
    }

    @Test
    void checkAuthorisationQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cases");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter("hearing_id", "2");

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByHearingId(2, roles);
    }

    @Test
    void checkAuthorisationHeaderParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cases");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.addHeader("hearing_id", "2");

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByHearingId(2, roles);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenHearingIdParameterMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cases");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_HEARING_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_HEARING_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

}
