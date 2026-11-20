package uk.gov.hmcts.darts.audio.service;

import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.darts.audio.config.AudioDiscoverySpikeConfig;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AudioDiscoverySpikeService {
    private final DataManagementAzureClientFactory factory;
    private final AudioDiscoverySpikeConfig config;

    public void scan(boolean cvp, boolean car, int batchSize) {
        List<String> containers = selectContainers(cvp, car, batchSize);
        UUID runId = UUID.randomUUID();
        long start = System.nanoTime();
        var storage = factory.getBlobServiceClient(config.getConnectionString());
        var options = new ListBlobsOptions().setPrefix(config.getPrefix());
        for (String container : containers) {
            long containerStart = System.nanoTime();
            int count = 0;
            try {
                var blobs = factory.getBlobContainerClient(container, storage)
                    .listBlobs(options, Duration.ofSeconds(30));
                var iterator = blobs.iterator();
                while (count < batchSize && iterator.hasNext()) {
                    iterator.next();
                    count++;
                }
                log.info("AudioDiscoverySpike run={} container={} count={} limitReached={} elapsedMs={} status=SUCCESS",
                         runId, container, count, count == batchSize, (System.nanoTime() - containerStart) / 1_000_000);
            } catch (RuntimeException exception) {
                log.warn("AudioDiscoverySpike run={} container={} count={} status=FAILED", runId, container, count);
                throw exception;
            }
        }
        log.info("AudioDiscoverySpike run={} containers={} elapsedMs={} status=SUCCESS",
                 runId, containers.size(), (System.nanoTime() - start) / 1_000_000);
    }

    private List<String> selectContainers(boolean cvp, boolean car, int batchSize) {
        validateSettings(batchSize);
        if (!cvp && !car) {
            throw new IllegalArgumentException("Select at least one source");
        }
        if (cvp) {
            requireContainer(config.getCvpContainer());
        }
        if (car) {
            requireContainer(config.getCarContainer());
        }
        var containers = new ArrayList<String>();
        if (cvp) {
            containers.add(config.getCvpContainer());
        }
        if (car) {
            containers.add(config.getCarContainer());
        }
        return containers;
    }

    private void validateSettings(int batchSize) {
        if (!StringUtils.hasText(config.getConnectionString()) || !StringUtils.hasText(config.getPrefix()) || batchSize <= 0) {
            throw new IllegalArgumentException("Configure spike storage, a non-empty prefix and a positive batch size");
        }
    }

    private void requireContainer(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Configure the selected spike container");
        }
    }
}
