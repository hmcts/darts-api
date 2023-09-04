package uk.gov.hmcts.darts.authorisation.component.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;

import static uk.gov.hmcts.darts.cases.exception.CaseApiError.CASE_NOT_FOUND;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorisationImpl implements Authorisation {

    private final CaseRepository caseRepository;
    private final AuthorisationApi authorisationApi;

    @Override
    public void authorise(Integer caseId) {
        try {
            final List<CourthouseEntity> courthouses = List.of(caseRepository.getReferenceById(caseId).getCourthouse());
            authorisationApi.checkAuthorisation(courthouses);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Case-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(CASE_NOT_FOUND);
        }
    }

}
