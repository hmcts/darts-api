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

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_MEDIA_REQUEST_ID;

@Component
@RequiredArgsConstructor
@Slf4j
class MediaRequestIdControllerAuthorisationImpl extends BaseControllerAuthorisation
      implements ControllerAuthorisation {

    static final String MEDIA_REQUEST_ID_PARAM = "media_request_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return MEDIA_REQUEST_ID;
    }

    public String getEntityIdParam() {
        return MEDIA_REQUEST_ID_PARAM;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> mediaRequestIdParamOptional = getEntityParamOptional(request, MEDIA_REQUEST_ID_PARAM);
        checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);

        if (mediaRequestIdParamOptional.isEmpty()) {
            log.error(String.format(
                  BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                  MEDIA_REQUEST_ID_PARAM,
                  request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_MEDIA_REQUEST_ID);
        }
        Integer mediaRequestId = Integer.valueOf(mediaRequestIdParamOptional.get());
        authorisation.authoriseMediaRequestAgainstUser(mediaRequestId);
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        checkAuthorisationByMediaRequestId(idToAuthorise.get(), roles);

        if (!idToAuthorise.get().isPresent()) {
            log.error(String.format(
                  BAD_REQUEST_AUTHORISATION_ID_ERROR_MESSAGE
            ));
            throw new DartsApiException(BAD_REQUEST_MEDIA_REQUEST_ID);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByMediaRequestId(jsonNode.path(MEDIA_REQUEST_ID_PARAM).intValue(), roles);
    }

    void checkAuthorisationByMediaRequestId(Optional<String> mediaRequestIdParamOptional,
                                            Set<SecurityRoleEnum> roles) {
        if (mediaRequestIdParamOptional.isPresent()) {
            try {
                Integer mediaRequestId = Integer.valueOf(mediaRequestIdParamOptional.get());
                authorisation.authoriseByMediaRequestId(mediaRequestId, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse audio_request_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_MEDIA_REQUEST_ID);
            }
        }
    }

}
