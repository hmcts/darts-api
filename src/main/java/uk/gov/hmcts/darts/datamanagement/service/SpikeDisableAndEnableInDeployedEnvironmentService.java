package uk.gov.hmcts.darts.datamanagement.service;

@FunctionalInterface
public interface SpikeDisableAndEnableInDeployedEnvironmentService {

    void process(int batchSize);
}
