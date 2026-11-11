package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.datamanagement.service.SpikeDisableAndEnableInDeployedEnvironmentService;

import java.time.Instant;

@Service
@Slf4j
public class SpikeDisableAndEnableInDeployedEnvironmentServiceImpl implements SpikeDisableAndEnableInDeployedEnvironmentService {

    @Override
    public void process(int batchSize) {
        log.info("SpikeDisableAndEnableInDeployedEnvironment executed at {} with batch size {}", Instant.now(), batchSize);
    }
}
