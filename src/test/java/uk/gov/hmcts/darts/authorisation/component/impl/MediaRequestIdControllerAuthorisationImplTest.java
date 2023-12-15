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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static uk.gov.hmcts.darts.authorisation.component.impl.MediaRequestIdControllerAuthorisationImpl.MEDIA_REQUEST_ID_PARAM;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@ExtendWith(MockitoExtension.class)
class MediaRequestIdControllerAuthorisationImplTest {

    private static final String METHOD = "POST";
    private static final String URI = "/audio-requests";
    private static final String MEDIA_REQUEST_ID_PARAM_VALUE = "4";

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
            TRANSLATION_QA,
            RCJ_APPEALS
        );
        controllerAuthorisation = new MediaRequestIdControllerAuthorisationImpl(authorisation);
    }

    @Test
    void getContextId() {
        assertEquals(MEDIA_REQUEST_ID, controllerAuthorisation.getContextId());
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

        verify(authorisation).authoriseByMediaRequestId(4, roles);
    }

    @Test
    void checkAuthorisationRequestBodyWhenMediaRequestIdMissing() throws JsonProcessingException {
        String body = """
            {
              "case_id": 1,
              "hearing_id": 2,
              "media_id": 3,
              "transcription_id": 5
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByMediaRequestId(0, roles);
    }

    @Test
    void checkAuthorisationPathParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, "/audio-requests/4");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(MEDIA_REQUEST_ID_PARAM, MEDIA_REQUEST_ID_PARAM_VALUE)
        );

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByMediaRequestId(4, roles);
    }

    @Test
    void checkAuthorisationQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter(MEDIA_REQUEST_ID_PARAM, MEDIA_REQUEST_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByMediaRequestId(4, roles);
    }

    @Test
    void checkAuthorisationHeaderParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.addHeader(MEDIA_REQUEST_ID_PARAM, MEDIA_REQUEST_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByMediaRequestId(4, roles);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenMediaRequestIdParameterMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_MEDIA_REQUEST_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_MEDIA_REQUEST_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationSupplierIdParameter() {
        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(() -> Optional.of(MEDIA_REQUEST_ID_PARAM_VALUE), roles));

        verify(authorisation).authoriseByMediaRequestId(4, roles);
    }

    @Test
    void checkAuthorisationSupplierIdMissingParameter() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(() -> Optional.empty(), roles)
        );

        assertEquals(BAD_REQUEST_MEDIA_REQUEST_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_MEDIA_REQUEST_ID, exception.getError());
        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenMediaRequestIdInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter(MEDIA_REQUEST_ID_PARAM, "");

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_MEDIA_REQUEST_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_MEDIA_REQUEST_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

}
