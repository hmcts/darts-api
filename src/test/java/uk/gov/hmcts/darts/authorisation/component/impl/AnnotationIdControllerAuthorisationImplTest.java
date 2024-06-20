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
import static uk.gov.hmcts.darts.authorisation.component.impl.AnnotationIdControllerAuthorisationImpl.ANNOTATION_ID_PARAM;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANNOTATION_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_ANNOTATION_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@ExtendWith(MockitoExtension.class)
class AnnotationIdControllerAuthorisationImplTest {

    private static final String METHOD = "POST";
    private static final String URI = "/annotations";
    private static final String ANNOTATION_ID_PARAM_VALUE = "5";

    @Mock
    private Authorisation authorisation;

    private final ObjectMapper mapper = new ObjectMapper();
    private Set<SecurityRoleEnum> roles;

    private ControllerAuthorisation controllerAuthorisation;

    @BeforeEach
    void setUp() {
        roles = Set.of(
            JUDICIARY,
            REQUESTER,
            APPROVER,
            TRANSCRIBER,
            TRANSLATION_QA,
            RCJ_APPEALS
        );
        controllerAuthorisation = new AnnotationIdControllerAuthorisationImpl(authorisation);
    }

    @Test
    void getContextId() {
        assertEquals(ANNOTATION_ID, controllerAuthorisation.getContextId());
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
              "annotation_id": 6
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByAnnotationId(6, roles);
    }

    @Test
    void checkAuthorisationRequestBodyWhenAnnotationIdMissing() throws JsonProcessingException {
        String body = """
            {
              "case_id": 1,
              "hearing_id": 2,
              "media_id": 3,
              "media_request_id": 4
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByAnnotationId(0, roles);
    }

    @Test
    void checkAuthorisationPathParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, "/annotations");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(ANNOTATION_ID_PARAM, ANNOTATION_ID_PARAM_VALUE)
        );

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByAnnotationId(5, roles);
    }

    @Test
    void checkAuthorisationQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter(ANNOTATION_ID_PARAM, ANNOTATION_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByAnnotationId(5, roles);
    }

    @Test
    void checkAuthorisationHeaderParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.addHeader(ANNOTATION_ID_PARAM, ANNOTATION_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByAnnotationId(5, roles);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenAnnotationIdParameterMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_ANNOTATION_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_ANNOTATION_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationSupplierIdParameter() {
        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(() -> Optional.of(ANNOTATION_ID_PARAM_VALUE), roles));

        verify(authorisation).authoriseByAnnotationId(5, roles);
    }

    @Test
    void checkAuthorisationSupplierIdMissingParameter() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(() -> Optional.empty(), roles)
        );

        assertEquals(BAD_REQUEST_ANNOTATION_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_ANNOTATION_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenTranscriptionIdInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter(ANNOTATION_ID_PARAM, "");

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_ANNOTATION_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_ANNOTATION_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

}
