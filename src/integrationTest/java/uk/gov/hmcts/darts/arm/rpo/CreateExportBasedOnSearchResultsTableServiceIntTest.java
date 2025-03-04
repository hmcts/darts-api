package uk.gov.hmcts.darts.arm.rpo;

import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CreateExportBasedOnSearchResultsTableServiceIntTest extends PostgresIntegrationBase {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";

    @MockitoBean
    private ArmRpoClient armRpoClient;

    @MockitoBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private CreateExportBasedOnSearchResultsTableService createExportBasedOnSearchResultsTableService;

    private final Duration pollDuration = Duration.ofHours(4);


    @Test
    void createExportBasedOnSearchResultsTable_ReturnsTrue() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setResponseStatus(0);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        OffsetDateTime pollCreatedTs = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(pollCreatedTs);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = createArmRpoExecutionDetailEntity(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);
        assertNull(armRpoExecutionDetail.getProductionName());
        assertNull(armRpoExecutionDetail.getPollingCreatedAt());

        var bearerAuth = "Bearer some-token";

        // when
        boolean result = createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(
            bearerAuth, armRpoExecutionDetail.getId(), createHeaderColumns(), PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertTrue(result);

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(PRODUCTION_NAME, armRpoExecutionDetailEntityUpdated.getProductionName());
        assertEquals(pollCreatedTs.truncatedTo(ChronoUnit.SECONDS), armRpoExecutionDetailEntityUpdated.getPollingCreatedAt().truncatedTo(ChronoUnit.SECONDS));
        assertThat(armRpoExecutionDetailEntityUpdated.getPollingCreatedAt()).isNotNull();
        assertEquals(ArmRpoStateEnum.CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsFalse_WhenInProgress() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(400);
        response.setIsError(false);
        response.setResponseStatus(2);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        OffsetDateTime pollCreatedTs = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(pollCreatedTs);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = createArmRpoExecutionDetailEntity(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        boolean result = createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(
            bearerAuth, armRpoExecutionDetail.getId(), createHeaderColumns(), PRODUCTION_NAME, pollDuration, userAccount);

        // then
        assertFalse(result);

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertNull(armRpoExecutionDetailEntityUpdated.getProductionName());
        assertEquals(pollCreatedTs.truncatedTo(ChronoUnit.SECONDS), armRpoExecutionDetailEntityUpdated.getPollingCreatedAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(ArmRpoStateEnum.CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.IN_PROGRESS.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    @Test
    void createExportBasedOnSearchResultsTable_ReturnsFalse_WhenPollingOutOfRange() {
        // given
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        response.setStatus(400);
        response.setIsError(false);
        response.setResponseStatus(2);
        when(armRpoClient.createExportBasedOnSearchResultsTable(anyString(), any())).thenReturn(response);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        OffsetDateTime pollCreatedTs = OffsetDateTime.now().minus(Duration.ofHours(5));
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = createArmRpoExecutionDetailEntity(userAccount);
        armRpoExecutionDetailEntity.setPollingCreatedAt(pollCreatedTs);
        armRpoExecutionDetailEntity.setProductionName(PRODUCTION_NAME);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class,
                                                       () -> createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(
                                                           bearerAuth, armRpoExecutionDetail.getId(), createHeaderColumns(), PRODUCTION_NAME, pollDuration,
                                                           userAccount));

        // then
        MatcherAssert.assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM createExportBasedOnSearchResultsTable: Polling can only run for a maximum of PT4H"));

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());

    }

    private List<MasterIndexFieldByRecordClassSchema> createHeaderColumns() {
        return List.of(
            createMasterIndexFieldByRecordClassSchema("200b9c27-b497-4977-82e7-1586b32a5871", "Record Class", "record_class", "string", false),
            createMasterIndexFieldByRecordClassSchema("90ee0e13-8639-4c4a-b542-66b6c8911549", "Archived Date", "ingestionDate", "date", true),
            createMasterIndexFieldByRecordClassSchema("a9b8daf2-d9ff-4815-b65a-f6ae2763b92c", "Client Identifier", "client_identifier", "string", false),
            createMasterIndexFieldByRecordClassSchema("109b6bf1-57a0-48ec-b22e-c7248dc74f91", "Contributor", "contributor", "string", false),
            createMasterIndexFieldByRecordClassSchema("893048bf-1e7c-4811-9abf-00cd77a715cf", "Record Date", "recordDate", "date", false),
            createMasterIndexFieldByRecordClassSchema("fdd0fcbb-da46-4af1-a627-ac255c12bb23", "ObjectId", "bf_012", "number", false)
        );
    }

    private MasterIndexFieldByRecordClassSchema createMasterIndexFieldByRecordClassSchema(
        String uuid, String displayName, String propertyName, String propertyType, boolean isMasked) {

        return MasterIndexFieldByRecordClassSchema.builder()
            .masterIndexField(uuid)
            .displayName(displayName)
            .propertyName(propertyName)
            .propertyType(propertyType)
            .isMasked(isMasked)
            .build();
    }

    private static @NotNull ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity(UserAccountEntity userAccount) {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        armRpoExecutionDetailEntity.setSearchId("searchId");
        armRpoExecutionDetailEntity.setSearchItemCount(6);
        armRpoExecutionDetailEntity.setStorageAccountId("storageAccountId");
        return armRpoExecutionDetailEntity;
    }

}
