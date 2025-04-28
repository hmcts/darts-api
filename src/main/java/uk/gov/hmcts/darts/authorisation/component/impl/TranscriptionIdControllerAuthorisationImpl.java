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

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_TRANSCRIPTION_ID;

@Component
@RequiredArgsConstructor
@Slf4j
class TranscriptionIdControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String TRANSCRIPTION_ID_PARAM = "transcription_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return TRANSCRIPTION_ID;
    }

    public String getEntityIdParam() {
        return TRANSCRIPTION_ID_PARAM;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> transcriptionIdParamOptional = getEntityParamOptional(request, getEntityIdParam());
        checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);

        if (transcriptionIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                TRANSCRIPTION_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_ID);
        }
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        checkAuthorisationByTranscriptionId(idToAuthorise.get(), roles);
        if (idToAuthorise.get().isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_ID_ERROR_MESSAGE
            ));
            throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_ID);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByTranscriptionId(jsonNode.path(TRANSCRIPTION_ID_PARAM).longValue(), roles);
    }

    void checkAuthorisationByTranscriptionId(Optional<String> transcriptionIdParamOptional,
                                             Set<SecurityRoleEnum> roles) {
        if (transcriptionIdParamOptional.isPresent()) {
            try {
                Long transcriptionId = Long.valueOf(transcriptionIdParamOptional.get());
                authorisation.authoriseByTranscriptionId(transcriptionId, roles);
            } catch (NumberFormatException ex) {
                log.error("Unable to parse transcription_id for checkAuthorisation", ex);
                throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_ID, ex);
            }
        }
    }

}
