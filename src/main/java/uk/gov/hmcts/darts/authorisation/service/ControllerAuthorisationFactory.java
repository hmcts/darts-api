package uk.gov.hmcts.darts.authorisation.service;

import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;

@FunctionalInterface
public interface ControllerAuthorisationFactory {

    ControllerAuthorisation getHandler(ContextIdEnum contextId);

}
