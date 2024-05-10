package uk.gov.hmcts.darts.authorisation.component.impl;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANNOTATION_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_ANNOTATION_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnotationIdControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String ANNOTATION_ID_PARAM = "annotation_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return ANNOTATION_ID;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> annotationIdParamOptional = getEntityParamOptional(request, ANNOTATION_ID_PARAM);
        checkAuthorisationByAnnotationId(annotationIdParamOptional, roles);

        if (annotationIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                ANNOTATION_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_ANNOTATION_ID);
        }
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        checkAuthorisationByAnnotationId(idToAuthorise.get(), roles);

        if (idToAuthorise.get().isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_ID_ERROR_MESSAGE
            ));
            throw new DartsApiException(BAD_REQUEST_ANNOTATION_ID);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByAnnotationId(jsonNode.path(ANNOTATION_ID_PARAM).intValue(), roles);
    }

    void checkAuthorisationByAnnotationId(Optional<String> annotationIdParamOptional, Set<SecurityRoleEnum> roles) {
        if (annotationIdParamOptional.isPresent()) {
            try {
                Integer annotationId = Integer.valueOf(annotationIdParamOptional.get());
                authorisation.authoriseByAnnotationId(annotationId, roles);
            } catch (NumberFormatException ex) {
                log.error("Unable to parse annotation_id for checkAuthorisation", ex);
                throw new DartsApiException(BAD_REQUEST_ANNOTATION_ID, ex);
            }
        }
    }

    public String getEntityIdParam() {
        return ANNOTATION_ID_PARAM;
    }

}
