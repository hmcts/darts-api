package uk.gov.hmcts.darts.authorisation.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A unit of work that performs a set of operations if and only if the auth passes for a
 * number of stipulated. This class offers a nice way to authorise against a set of bespoke request
 * body ids as an alternative to the statically defined authorisation annotation
 * {@link uk.gov.hmcts.darts.authorisation.annotation.Authorisation}
 */
@Component
@RequiredArgsConstructor
public class AuthorisationUnitOfWork {

    private final List<ControllerAuthorisation> authorisation;

    public <T> void authoriseWithIdsForTranscription(List<T> idTypes, Function<T, String> getId,
                                                     SecurityRoleEnum[] roles, Runnable runnableOnAuth) {
        authoriseWithIds(idTypes, getId, ContextIdEnum.TRANSCRIPTION_ID, roles, runnableOnAuth, true);
    }


    public <T> void authoriseWithIds(List<T> idTypes, Function<T, String> gatherId,
                                     ContextIdEnum contextIdEnum,
                                     SecurityRoleEnum[] roles, Runnable runnable,
                                     boolean suppressDataValidation) {
        authorisation.forEach(auth -> {
            if (auth.getContextId() == contextIdEnum) {
                idTypes.forEach(idType -> {
                    try {
                        auth.checkAuthorisation(() -> Optional.of(gatherId.apply(idType)), new HashSet<>(Arrays.asList(roles)));
                    } catch (DartsApiException ex) {

                        // if the client wants to handle the ids being missing themselves then
                        // we can suppress this check
                        if (suppressDataValidation) {
                            if (ex.getError().getHttpStatus() == HttpStatus.FORBIDDEN) {
                                throw ex;
                            }
                        } else {
                            throw ex;
                        }
                    }
                });

                // once we have authorised all ids against the roles then execute the runnable
                runnable.run();
            }
        });
    }
}
