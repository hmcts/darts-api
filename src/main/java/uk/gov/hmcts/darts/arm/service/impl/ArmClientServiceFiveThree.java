package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "darts.storage.arm-api",
    name = "active-version",
    havingValue = "v5_3"
)
@AllArgsConstructor
public class ArmClientServiceFiveThree {

}
