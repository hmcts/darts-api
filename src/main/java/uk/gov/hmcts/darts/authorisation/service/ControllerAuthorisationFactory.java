package uk.gov.hmcts.darts.authorisation.service;

import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;

public interface ControllerAuthorisationFactory {

    ControllerAuthorisation getHandler(ContextIdEnum contextId);

}
