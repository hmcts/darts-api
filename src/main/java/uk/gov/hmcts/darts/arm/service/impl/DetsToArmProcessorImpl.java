package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.DetsToArmProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetsToArmProcessorImpl implements DetsToArmProcessor {
    @Override
    public void processDetsToArm(int batchSize) {

    }
}