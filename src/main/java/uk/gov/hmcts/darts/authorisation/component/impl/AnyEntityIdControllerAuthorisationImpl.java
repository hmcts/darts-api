package uk.gov.hmcts.darts.authorisation.component.impl;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
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

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_ANY_ID;

@Component
@AllArgsConstructor
@Slf4j
public class AnyEntityIdControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    private final Authorisation authorisation;

    private final CaseIdControllerAuthorisationImpl caseIdControllerAuthorisation;
    private final HearingIdControllerAuthorisationImpl hearingIdControllerAuthorisation;
    private final MediaIdControllerAuthorisationImpl mediaIdControllerAuthorisation;
    private final MediaRequestIdControllerAuthorisationImpl mediaRequestIdControllerAuthorisation;
    private final TranscriptionIdControllerAuthorisationImpl transcriptionIdControllerAuthorisation;
    private final TransformedMediaIdControllerAuthorisationImpl transformedMediaIdControllerAuthorisation;
    private final AnnotationIdControllerAuthorisationImpl annotationIdControllerAuthorisation;

    @Override
    public ContextIdEnum getContextId() {
        return ANY_ENTITY_ID;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> hearingIdParamOptional = getEntityParamOptional(request, hearingIdControllerAuthorisation.getEntityIdParam());
        hearingIdControllerAuthorisation.checkAuthorisationByHearingId(hearingIdParamOptional, roles);

        Optional<String> caseIdParamOptional = getEntityParamOptional(request, caseIdControllerAuthorisation.getEntityIdParam());
        caseIdControllerAuthorisation.checkAuthorisationByCaseId(caseIdParamOptional, roles);

        Optional<String> mediaIdParamOptional = getEntityParamOptional(request, mediaIdControllerAuthorisation.getEntityIdParam());
        mediaIdControllerAuthorisation.checkAuthorisationByMediaId(mediaIdParamOptional, roles);

        Optional<String> mediaRequestIdParamOptional = getEntityParamOptional(request, mediaRequestIdControllerAuthorisation.getEntityIdParam());
        mediaRequestIdControllerAuthorisation.checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);

        Optional<String> transcriptionIdParamOptional = getEntityParamOptional(request, transcriptionIdControllerAuthorisation.getEntityIdParam());
        transcriptionIdControllerAuthorisation.checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);

        Optional<String> transformedMediaIdParamOptional = getEntityParamOptional(request, transcriptionIdControllerAuthorisation.getEntityIdParam());
        transformedMediaIdControllerAuthorisation.checkAuthorisationByTransformedMediaId(transformedMediaIdParamOptional, roles);

        Optional<String> annotationIdParamOptional = getEntityParamOptional(request, annotationIdControllerAuthorisation.getEntityIdParam());
        annotationIdControllerAuthorisation.checkAuthorisationByAnnotationId(annotationIdParamOptional, roles);

        if (hearingIdParamOptional.isEmpty()
            && caseIdParamOptional.isEmpty()
            && mediaIdParamOptional.isEmpty()
            && mediaRequestIdParamOptional.isEmpty()
            && transcriptionIdParamOptional.isEmpty()
            && transformedMediaIdParamOptional.isEmpty()
            && annotationIdParamOptional.isEmpty()
        ) {
            entitiesNotFound("parameters");
        }
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings({"PMD.NPathComplexity"})
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        boolean entityExists = false;
        if (checkEntityExists(jsonNode, hearingIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByHearingId(jsonNode.path(hearingIdControllerAuthorisation.getEntityIdParam()).intValue(), roles);
            entityExists = true;
        }

        if (checkEntityExists(jsonNode, caseIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByCaseId(jsonNode.path(caseIdControllerAuthorisation.getEntityIdParam()).intValue(), roles);
            entityExists = true;
        }

        if (checkEntityExists(jsonNode, mediaIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByMediaId(jsonNode.path(mediaIdControllerAuthorisation.getEntityIdParam()).longValue(), roles);
            entityExists = true;
        }

        if (checkEntityExists(jsonNode, mediaRequestIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByMediaRequestId(jsonNode.path(mediaRequestIdControllerAuthorisation.getEntityIdParam()).intValue(), roles);
            entityExists = true;
        }

        if (checkEntityExists(jsonNode, transcriptionIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByTranscriptionId(jsonNode.path(transcriptionIdControllerAuthorisation.getEntityIdParam()).longValue(), roles);
            entityExists = true;
        }

        if (checkEntityExists(jsonNode, transformedMediaIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByTransformedMediaId(jsonNode.path(transformedMediaIdControllerAuthorisation.getEntityIdParam()).intValue(), roles);
            entityExists = true;
        }

        if (checkEntityExists(jsonNode, annotationIdControllerAuthorisation.getEntityIdParam())) {
            authorisation.authoriseByAnnotationId(jsonNode.path(annotationIdControllerAuthorisation.getEntityIdParam()).intValue(), roles);
            entityExists = true;
        }

        if (!entityExists) {
            entitiesNotFound("request body");
        }
    }

    private boolean checkEntityExists(JsonNode jsonNode, String entityIdParam) {
        return jsonNode.path(entityIdParam) != null
            && jsonNode.path(entityIdParam).intValue() != 0;
    }

    private static void entitiesNotFound(String authLocation) {
        log.error("Unable to find entity/entities in {} for authorisation", authLocation);
        throw new DartsApiException(BAD_REQUEST_ANY_ID);
    }

}
