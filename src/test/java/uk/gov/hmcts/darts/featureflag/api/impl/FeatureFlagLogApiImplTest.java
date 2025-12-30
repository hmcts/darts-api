package uk.gov.hmcts.darts.featureflag.api.impl;

import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.featureflag.config.FeatureFlagLoggingConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Isolated
@ExtendWith(MockitoExtension.class)
@Slf4j
class FeatureFlagLogApiImplTest {

    private static final String LOG_MESSAGE = "test message ";
    private static final String LOG_MESSAGE_RPO = LOG_MESSAGE + "RPO";
    private static final String LOG_MESSAGE_DETS_TO_ARM_PUSH = LOG_MESSAGE + "DETS_TO_ARM_PUSH";
    private static final String LOG_MESSAGE_DETS_TO_ARM_PULL = LOG_MESSAGE + "DETS_TO_ARM_PULL";

    private static LogCaptor logCaptor;

    @Mock
    private FeatureFlagLoggingConfig featureFlagLoggingConfig;

    private FeatureFlagLogApiImpl featureFlagLogApi;

    @BeforeEach
    void setUp() {
        featureFlagLogApi = new FeatureFlagLogApiImpl(featureFlagLoggingConfig);
    }

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(FeatureFlagLogApiImpl.class);
        logCaptor.setLogLevelToInfo();
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @Test
    void logArmRpo_ShouldLogMessage_WhenIsEnableArmRpoFeatureFlagLogsEnabled() {
        when(featureFlagLoggingConfig.isEnableArmRpoFeatureFlagLogs()).thenReturn(true);
        featureFlagLogApi.logArmRpo(LOG_MESSAGE_RPO);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(LOG_MESSAGE_RPO, infoLogs.getFirst());
    }

    @Test
    void logArmRpo_ShouldNotLogMessage_WhenIsEnableArmRpoFeatureFlagLogsDisabled() {
        when(featureFlagLoggingConfig.isEnableArmRpoFeatureFlagLogs()).thenReturn(false);
        featureFlagLogApi.logArmRpo(LOG_MESSAGE_RPO);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(0, infoLogs.size());
    }

    @Test
    void logDetsToArmPush_ShouldLogMessage_WhenIsEnableDetsToArmPushFeatureFlagLogsEnabled() {
        when(featureFlagLoggingConfig.isEnableDetsToArmPushFeatureFlagLogs()).thenReturn(true);
        featureFlagLogApi.logDetsToArmPush(LOG_MESSAGE_DETS_TO_ARM_PUSH);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(LOG_MESSAGE_DETS_TO_ARM_PUSH, infoLogs.getFirst());
    }

    @Test
    void logArmRpo_ShouldNotLogMessage_WhenIsEnableDetsToArmPushFeatureFlagLogsDisabled() {
        when(featureFlagLoggingConfig.isEnableDetsToArmPushFeatureFlagLogs()).thenReturn(false);
        featureFlagLogApi.logDetsToArmPush(LOG_MESSAGE_DETS_TO_ARM_PUSH);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(0, infoLogs.size());
    }

    @Test
    void logDetsToArmPull_ShouldLogMessage_WhenIsEnableDetsToArmPullFeatureFlagLogsEnabled() {
        when(featureFlagLoggingConfig.isEnableDetsToArmPullFeatureFlagLogs()).thenReturn(true);
        featureFlagLogApi.logDetsToArmPull(LOG_MESSAGE_DETS_TO_ARM_PULL);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(LOG_MESSAGE_DETS_TO_ARM_PULL, infoLogs.getFirst());
    }

    @Test
    void logArmRpo_ShouldNotLogMessage_WhenIsEnableDetsToArmPullFeatureFlagLogsDisabled() {
        when(featureFlagLoggingConfig.isEnableDetsToArmPullFeatureFlagLogs()).thenReturn(false);
        featureFlagLogApi.logDetsToArmPull(LOG_MESSAGE_DETS_TO_ARM_PULL);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(0, infoLogs.size());
    }
}
