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
import static uk.gov.hmcts.darts.authorisation.component.impl.CaseIdControllerAuthorisationImpl.CASE_ID_PARAM;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.CASE_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_CASE_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@ExtendWith(MockitoExtension.class)
class CaseIdControllerAuthorisationImplTest {

    private static final String METHOD = "POST";
    private static final String URI = "/cases";
    private static final String CASE_ID_PARAM_VALUE = "1";

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
        controllerAuthorisation = new CaseIdControllerAuthorisationImpl(authorisation);
    }

    @Test
    void getContextId() {
        assertEquals(CASE_ID, controllerAuthorisation.getContextId());
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

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationRequestBodyWhenCaseIdMissing() throws JsonProcessingException {
        String body = """
            {
              "hearing_id": 2,
              "media_id": 3,
              "media_request_id": 4,
              "transcription_id": 5
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByCaseId(0, roles);
    }

    @Test
    void checkAuthorisationPathParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, "/cases/1");
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(CASE_ID_PARAM, CASE_ID_PARAM_VALUE)
        );

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter(CASE_ID_PARAM, CASE_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationHeaderParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.addHeader(CASE_ID_PARAM, CASE_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationSupplierIdParameter() {
        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(() -> Optional.of(CASE_ID_PARAM_VALUE), roles));

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationSupplierIdMissingParameter() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(() -> Optional.empty(), roles)
        );

        assertEquals(BAD_REQUEST_CASE_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_CASE_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenCaseIdParameterMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_CASE_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_CASE_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenCaseIdInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.setParameter(CASE_ID_PARAM, "");

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_CASE_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_CASE_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

}
