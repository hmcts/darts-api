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
import static uk.gov.hmcts.darts.authorisation.component.impl.CaseIdControllerAuthorisationImpl.CASE_ID_PARAM;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_ANY_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_CASE_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@ExtendWith(MockitoExtension.class)
class AnyEntityIdControllerAuthorisationImplTest {

    private static final String METHOD = "POST";
    private static final String CASES_URI = "/cases";
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
        CaseIdControllerAuthorisationImpl caseIdControllerAuthorisation =
            new CaseIdControllerAuthorisationImpl(authorisation);
        HearingIdControllerAuthorisationImpl hearingIdControllerAuthorisation =
            new HearingIdControllerAuthorisationImpl(authorisation);
        MediaIdControllerAuthorisationImpl mediaIdControllerAuthorisation =
            new MediaIdControllerAuthorisationImpl(authorisation);
        MediaRequestIdControllerAuthorisationImpl mediaRequestIdControllerAuthorisation =
            new MediaRequestIdControllerAuthorisationImpl(authorisation);
        TranscriptionIdControllerAuthorisationImpl transcriptionIdControllerAuthorisation =
            new TranscriptionIdControllerAuthorisationImpl(authorisation);
        TransformedMediaIdControllerAuthorisationImpl transformedMediaIdControllerAuthorisation =
            new TransformedMediaIdControllerAuthorisationImpl(authorisation);
        AnnotationIdControllerAuthorisationImpl annotationIdControllerAuthorisation =
            new AnnotationIdControllerAuthorisationImpl(authorisation);
        controllerAuthorisation = new AnyEntityIdControllerAuthorisationImpl(
            authorisation,
            caseIdControllerAuthorisation,
            hearingIdControllerAuthorisation,
            mediaIdControllerAuthorisation,
            mediaRequestIdControllerAuthorisation,
            transcriptionIdControllerAuthorisation,
            transformedMediaIdControllerAuthorisation,
            annotationIdControllerAuthorisation
        );
    }

    @Test
    void getContextId() {
        assertEquals(ANY_ENTITY_ID, controllerAuthorisation.getContextId());
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

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationRequestBodyWhenCaseIdMissing() throws JsonProcessingException {
        String body = """
            {
              "hearing_id": 2,
              "media_id": 3,
              "media_request_id": 4,
              "transcription_id": 5,
              "transformed_media_id": 6,
              "annotation_id": 7
            }
            """;

        JsonNode jsonNode = mapper.readTree(body);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(jsonNode, roles));

        verify(authorisation).authoriseByHearingId(2, roles);
        verify(authorisation).authoriseByMediaId(3L, roles);
        verify(authorisation).authoriseByMediaRequestId(4, roles);
        verify(authorisation).authoriseByTranscriptionId(5L, roles);
        verify(authorisation).authoriseByTransformedMediaId(6, roles);
        verify(authorisation).authoriseByAnnotationId(7, roles);
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
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, CASES_URI);
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
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, CASES_URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );
        request.addHeader(CASE_ID_PARAM, CASE_ID_PARAM_VALUE);

        assertDoesNotThrow(() -> controllerAuthorisation.checkAuthorisation(request, roles));

        verify(authorisation).authoriseByCaseId(1, roles);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenCaseIdParameterMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, CASES_URI);
        request.setAttribute(
            URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Collections.emptyMap()
        );

        var exception = assertThrows(
            DartsApiException.class,
            () -> controllerAuthorisation.checkAuthorisation(request, roles)
        );

        assertEquals(BAD_REQUEST_ANY_ID.getTitle(), exception.getMessage());
        assertEquals(BAD_REQUEST_ANY_ID, exception.getError());

        verifyNoInteractions(authorisation);
    }

    @Test
    void checkAuthorisationShouldThrowBadRequestWhenCaseIdInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, CASES_URI);
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
