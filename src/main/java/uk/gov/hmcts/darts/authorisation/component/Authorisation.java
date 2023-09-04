package uk.gov.hmcts.darts.authorisation.component;

import org.springframework.stereotype.Component;

@Component
public interface Authorisation {

    void authorise(Integer caseId);

}
