package uk.gov.hmcts.darts.authorisation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.authorisation.service.ControllerAuthorisationFactory;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ControllerAuthorisationFactoryImpl implements ControllerAuthorisationFactory {

    private final List<ControllerAuthorisation> handlers;

    @Override
    public ControllerAuthorisation getHandler(ContextIdEnum contextId) {
        return handlers
              .stream()
              .filter(handler -> contextId.equals(handler.getContextId()))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException(String.format(
                    "The Authorisation annotation contextId is not known: %s",
                    contextId
              )));
    }

}
