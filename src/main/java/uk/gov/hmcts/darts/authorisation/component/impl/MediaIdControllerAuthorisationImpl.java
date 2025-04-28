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

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_MEDIA_ID;

@Component
@RequiredArgsConstructor
@Slf4j
class MediaIdControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String MEDIA_ID_PARAM = "media_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return MEDIA_ID;
    }

    public String getEntityIdParam() {
        return MEDIA_ID_PARAM;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> mediaIdParamOptional = getEntityParamOptional(request, MEDIA_ID_PARAM);
        checkAuthorisationByMediaId(mediaIdParamOptional, roles);

        if (mediaIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                MEDIA_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_MEDIA_ID);
        }
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        checkAuthorisationByMediaId(idToAuthorise.get(), roles);

        if (!idToAuthorise.get().isPresent()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_ID_ERROR_MESSAGE
            ));
            throw new DartsApiException(BAD_REQUEST_MEDIA_ID);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByMediaId(jsonNode.path(MEDIA_ID_PARAM).longValue(), roles);
    }

    void checkAuthorisationByMediaId(Optional<String> mediaIdParamOptional, Set<SecurityRoleEnum> roles) {
        if (mediaIdParamOptional.isPresent()) {
            try {
                Long mediaId = Long.valueOf(mediaIdParamOptional.get());
                authorisation.authoriseByMediaId(mediaId, roles);
            } catch (NumberFormatException ex) {
                log.error("Unable to parse media_id for checkAuthorisation", ex);
                throw new DartsApiException(BAD_REQUEST_MEDIA_ID, ex);
            }
        }
    }

}
