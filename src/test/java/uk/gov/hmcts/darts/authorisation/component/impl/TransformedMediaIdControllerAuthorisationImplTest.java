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
import static uk.gov.hmcts.darts.authorisation.component.impl.TransformedMediaIdControllerAuthorisationImpl.TRANSFORMED_MEDIA_ID_PARAM;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSFORMED_MEDIA_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_TRANSFORMED_MEDIA_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@ExtendWith(MockitoExtension.class)
class TransformedMediaIdControllerAuthorisationImplTest {

    private static final String METHOD = "POST";
    private static final String URI = "/audio-requests";
    private static final String TRANSFORMED_MEDIA_ID_PARAM_VALUE = "6";
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock
    private Authorisation authorisation;
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
        controllerAuthorisation = new TransformedMediaIdControllerAuthorisationImpl(authorisation);
    }

    @Test
    void getContextId() {
        assertEquals(TRANSFORMED_MEDIA_ID, controllerAuthorisation.getContextId());
    }

    @Test
    void checkAuthorisationRequestBody() throws JsonProcessingException {
        String body = """
              {
                "case_id": 1,
                "hearing_id": 2,
                "media_id": 3,
                "media_request_id": 4,
                "transcription_id": 5,
                "transformed_media_id": 6
              }
              """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByTransformedMediaId(6, roles);
    }

    @Test
    void checkAuthorisationRequestBodyWhenTransformedMediaIdMissing() throws JsonProcessingException {
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

        verify(authorisation).authoriseByTransformedMediaId(0, roles);
    }

    @Test
    void checkAuthorisationPathParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, "/audio-requests/transformed_media/6");
        request.setAttribute(
              URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(TRANSFORMED_MEDIA_ID_PARAM, TRANSFORMED_MEDIA_ID_PARAM_VALUE)
        );

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByTransformedMediaId(6, roles);
    }

    @Test
    void checkAuthorisationQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
              URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              Collections.emptyMap()
        );
        request.setParameter(TRANSFORMED_MEDIA_ID_PARAM, TRANSFORMED_MEDIA_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByTransformedMediaId(6, roles);
    }

    @Test
    void checkAuthorisationHeaderParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
              URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              Collections.emptyMap()
        );
        request.addHeader(TRANSFORMED_MEDIA_ID_PARAM, TRANSFORMED_MEDIA_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByTransformedMediaId(6, roles);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenTransformedMediaIdParameterMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
              URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              Collections.emptyMap()
        );

        var exception = assertThrows(
              DartsApiException.class,
              () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_TRANSFORMED_MEDIA_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_TRANSFORMED_MEDIA_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationSupplierIdParameter() {
        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(() -> Optional.of(TRANSFORMED_MEDIA_ID_PARAM_VALUE), roles));

        verify(authorisation).authoriseByTransformedMediaId(6, roles);
    }

    @Test
    void checkAuthorisationSupplierIdMissingParameter() {
        var exception = assertThrows(
              DartsApiException.class,
              () -> controllerAuthorisation.checkAuthorisation(Optional::empty, roles)
        );

        assertEquals(BAD_REQUEST_TRANSFORMED_MEDIA_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_TRANSFORMED_MEDIA_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenTransformedMediaIdInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
              URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              Collections.emptyMap()
        );
        request.setParameter(TRANSFORMED_MEDIA_ID_PARAM, "");

        var exception = assertThrows(
              DartsApiException.class,
              () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_TRANSFORMED_MEDIA_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_TRANSFORMED_MEDIA_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

}
