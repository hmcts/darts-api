package uk.gov.hmcts.darts.annotation.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;

import java.util.List;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.ANNOTATION_NOT_FOUND;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.NOT_AUTHORISED_TO_DELETE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@RequiredArgsConstructor
@Component
public class UserAuthorisedToDeleteAnnotationValidator implements Validator<Integer> {

    private final AnnotationRepository annotationRepository;
    private final AuthorisationApi authorisationApi;

    @Override
    public void validate(Integer id) {
        var annotation = annotationRepository.findById(id).orElseThrow(() -> new DartsApiException(ANNOTATION_NOT_FOUND));
        var currentUser = authorisationApi.getCurrentUser();
        if (!authorisationApi.userHasOneOfRoles(List.of(ADMIN)) && !annotation.isOwnedBy(currentUser)) {
            throw new DartsApiException(NOT_AUTHORISED_TO_DELETE);
        }
    }
}
