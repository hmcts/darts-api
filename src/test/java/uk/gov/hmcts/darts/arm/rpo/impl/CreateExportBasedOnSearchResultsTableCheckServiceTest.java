package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateExportBasedOnSearchResultsTableCheckServiceTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private CreateExportBasedOnSearchResultsTableServiceImpl createExportBasedOnSearchResultsTableService;

    @Mock
    private UserAccountEntity userAccount;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private final Duration pollDuration = Duration.ofHours(4);

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);

        createExportBasedOnSearchResultsTableService = new CreateExportBasedOnSearchResultsTableServiceImpl(
            armRpoClient, armRpoService, armRpoUtil, currentTimeHelper, objectMapper);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setSearchId("searchId");
        armRpoExecutionDetailEntity.setSearchItemCount(7);
        armRpoExecutionDetailEntity.setProductionId("productionId");
        armRpoExecutionDetailEntity.setStorageAccountId("storageAccountId");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

    }

    @Test
    void checkCreateExportBasedOnSearchResultsInProgress_PollingStillInProgress() {
        // given
        armRpoExecutionDetailEntity.setPollingCreatedAt(OffsetDateTime.now().minusMinutes(10));
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, false, 2);

        // when
        boolean result = createExportBasedOnSearchResultsTableService.checkCreateExportBasedOnSearchResultsInProgress(userAccount, response,
                                                                                                                      new StringBuilder(),
                                                                                                                      armRpoExecutionDetailEntity,
                                                                                                                      pollDuration);

        // then
        assertFalse(result);
        verify(armRpoService, never()).updateArmRpoStatus(any(), any(), any());
    }

    @Test
    void checkCreateExportBasedOnSearchResultsInProgress_PollingExceeded() {
        //given
        armRpoExecutionDetailEntity.setPollingCreatedAt(OffsetDateTime.now().minusHours(5));
        CreateExportBasedOnSearchResultsTableResponse response = createResponse(400, false, 2);

        // when
        StringBuilder errorMessage = new StringBuilder();
        assertThrows(ArmRpoException.class, () ->
            createExportBasedOnSearchResultsTableService.checkCreateExportBasedOnSearchResultsInProgress(userAccount, response, errorMessage,
                                                                                                         armRpoExecutionDetailEntity, pollDuration));

        // then
        verify(armRpoService).updateArmRpoStatus(any(), any(), any());
    }

    private CreateExportBasedOnSearchResultsTableResponse createResponse(int status, boolean isError, int responseStatus) {
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(status);
        response.setIsError(isError);
        response.setResponseStatus(responseStatus);
        return response;
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}