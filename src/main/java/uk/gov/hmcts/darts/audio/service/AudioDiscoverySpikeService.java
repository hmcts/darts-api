package uk.gov.hmcts.darts.audio.service;

import com.azure.storage.blob.BlobServiceClient;
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
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class AudioDiscoverySpikeService {
    private static final String CVP = "CVP";
    private static final String CAR = "CAR";

    private final DataManagementAzureClientFactory factory;
    private final AudioDiscoverySpikeConfig config;

    // Retains the combined task's existing prefixes, sequence and batch limits.
    public void scan(boolean cvp, boolean car, int batchSize) {
        validateSettings(config.getConnectionString(), batchSize);
        if (!StringUtils.hasText(config.getPrefix())) {
            throw new IllegalArgumentException("Configure the combined scan prefix");
        }
        if (!cvp && !car) {
            throw new IllegalArgumentException("Select at least one source");
        }
        var sources = new ArrayList<Source>();
        if (cvp) {
            sources.add(new Source(CVP, config.getCvpContainer(), config.getPrefix(), true));
        }
        if (car) {
            sources.add(new Source(CAR, config.getCarContainer(), config.getPrefix(), true));
        }
        run("COMBINED", sources, () -> factory.getBlobServiceClient(config.getConnectionString()), batchSize);
    }

    public void scanCvp(int batchSize) {
        validateSettings(config.getConnectionString(), batchSize);
        run(CVP, List.of(new Source(CVP, config.getCvpContainer(), "", false)),
            () -> factory.getBlobServiceClient(config.getConnectionString()), batchSize);
    }

    public void scanCar(int batchSize) {
        validateSettings(config.getConnectionString(), batchSize);
        run(CAR, List.of(new Source(CAR, config.getCarContainer(), config.getCarPrefix(), false)),
            () -> factory.getBlobServiceClient(config.getConnectionString()), batchSize);
    }

    public void scanCvpExternal(int batchSize) {
        validateSettings(config.getCvpAccountUrl(), batchSize);
        if (!StringUtils.hasText(config.getCvpPrefix())) {
            throw new IllegalArgumentException("Configure the external CVP prefix");
        }
        run("CVP_EXTERNAL", List.of(new Source("CVP_EXTERNAL", config.getCvpSourceContainer(), config.getCvpPrefix(), true)),
            () -> factory.getBlobServiceClientWithIdentity(config.getCvpAccountUrl()), batchSize);
    }

    public void scanCombinedFaulty(int batchSize) {
        validateSettings(config.getConnectionString(), batchSize);
        run("COMBINED_FAULTY", List.of(new Source(CVP, "darts-container", "", false),
                                       new Source(CAR, config.getCarContainer(), config.getCarPrefix(), false)),
            () -> factory.getBlobServiceClient(config.getConnectionString()), batchSize);
    }

    private void run(String mode, List<Source> sources, Supplier<BlobServiceClient> storage, int batchSize) {
        UUID runId = UUID.randomUUID();
        long start = System.nanoTime();
        log.info("AudioDiscoverySpike run={} mode={} containers={} status=STARTED", runId, mode, sources.size());
        for (int index = 0; index < sources.size(); index++) {
            try {
                scanSource(runId, mode, sources.get(index), storage, batchSize);
            } catch (RuntimeException exception) {
                logNotAttempted(runId, mode, sources.subList(index + 1, sources.size()), sources.get(index));
                log.warn("AudioDiscoverySpike run={} mode={} containers={} attempted={} elapsedMs={} status=FAILED",
                         runId, mode, sources.size(), index + 1, elapsed(start));
                throw exception;
            }
        }
        log.info("AudioDiscoverySpike run={} mode={} containers={} elapsedMs={} status=SUCCESS",
                 runId, mode, sources.size(), elapsed(start));
    }

    private void scanSource(UUID runId, String mode, Source source, Supplier<BlobServiceClient> storage, int batchSize) {
        long start = System.nanoTime();
        long count = 0;
        log.info("AudioDiscoverySpike run={} mode={} source={} container={} prefix={} status=STARTED",
                 runId, mode, source.name(), source.container(), source.prefix());
        try {
            var container = factory.getBlobContainerClient(source.container(), storage.get());
            var options = new ListBlobsOptions().setPrefix(source.prefix());
            var iterator = container.listBlobs(options, Duration.ofSeconds(30)).iterator();
            long limit = source.bounded() ? batchSize : Long.MAX_VALUE;
            while (count < limit && iterator.hasNext()) {
                iterator.next();
                count++;
            }
            log.info("AudioDiscoverySpike run={} mode={} source={} container={} count={} limitReached={} elapsedMs={} status=SUCCESS",
                     runId, mode, source.name(), source.container(), count, source.bounded() && count == limit, elapsed(start));
        } catch (RuntimeException exception) {
            log.warn("AudioDiscoverySpike run={} mode={} source={} container={} count={} elapsedMs={} errorType={} status=FAILED",
                     runId, mode, source.name(), source.container(), count, elapsed(start), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }

    private void logNotAttempted(UUID runId, String mode, List<Source> remaining, Source failed) {
        for (Source source : remaining) {
            log.warn("AudioDiscoverySpike run={} mode={} source={} container={} count=unknown elapsedMs=0 "
                         + "status=NOT_ATTEMPTED reason=PREVIOUS_SOURCE_FAILED failedSource={}",
                     runId, mode, source.name(), source.container(), failed.name());
        }
    }

    private void validateSettings(String connection, int batchSize) {
        if (!StringUtils.hasText(connection) || batchSize <= 0) {
            throw new IllegalArgumentException("Configure spike storage and a positive batch size");
        }
    }

    private long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private record Source(String name, String container, String prefix, boolean bounded) {
        private Source {
            if (!StringUtils.hasText(container)) {
                throw new IllegalArgumentException("Configure the selected spike container");
            }
            prefix = prefix == null ? "" : prefix;
        }
    }
}
