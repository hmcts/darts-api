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

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSFORMED_MEDIA_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_TRANSFORMED_MEDIA_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransformedMediaIdControllerAuthorisationImpl extends BaseControllerAuthorisation
      implements ControllerAuthorisation {

    static final String TRANSFORMED_MEDIA_ID_PARAM = "transformed_media_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return TRANSFORMED_MEDIA_ID;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> transformedMediaIdParamOptional = getEntityParamOptional(request, TRANSFORMED_MEDIA_ID_PARAM);
        checkAuthorisationByTransformedMediaId(transformedMediaIdParamOptional, roles);

        if (transformedMediaIdParamOptional.isEmpty()) {
            log.error(String.format(
                  BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                  TRANSFORMED_MEDIA_ID_PARAM,
                  request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_TRANSFORMED_MEDIA_ID);
        }
        authorisation.authoriseTransformedMediaAgainstUser(Integer.valueOf(transformedMediaIdParamOptional.get()));
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        checkAuthorisationByTransformedMediaId(idToAuthorise.get(), roles);

        if (idToAuthorise.get().isEmpty()) {
            log.error(String.format(
                  BAD_REQUEST_AUTHORISATION_ID_ERROR_MESSAGE
            ));
            throw new DartsApiException(BAD_REQUEST_TRANSFORMED_MEDIA_ID);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByTransformedMediaId(jsonNode.path(TRANSFORMED_MEDIA_ID_PARAM).intValue(), roles);
    }

    void checkAuthorisationByTransformedMediaId(Optional<String> transformedMediaIdParamOptional,
                                                Set<SecurityRoleEnum> roles) {
        if (transformedMediaIdParamOptional.isPresent()) {
            try {
                Integer transformedMedia = Integer.valueOf(transformedMediaIdParamOptional.get());
                authorisation.authoriseByTransformedMediaId(transformedMedia, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse transformed_media_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_TRANSFORMED_MEDIA_ID);
            }
        }
    }

    public String getEntityIdParam() {
        return TRANSFORMED_MEDIA_ID_PARAM;
    }

}
