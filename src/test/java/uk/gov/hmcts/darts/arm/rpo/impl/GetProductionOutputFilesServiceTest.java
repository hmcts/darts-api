package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.rpo.GetProductionOutputFilesService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.IN_PROGRESS_STATUS;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.READY_STATUS;

@TestPropertySource(properties = {"darts.storage.arm.is-mock-arm-rpo-download-csv=true"})
@SuppressWarnings("checkstyle:linelength")
@ExtendWith(MockitoExtension.class)
class GetProductionOutputFilesServiceTest {

    private GetProductionOutputFilesService getProductionOutputFilesService;
    private ArmRpoService armRpoService;
    private ArmRpoClient armRpoClient;

    private ArmRpoHelperMocks armRpoHelperMocks;

    private static final Integer EXECUTION_ID = 1;
    private static final String TOKEN = "some token";
    private static final String PRODUCTION_ID = UUID.randomUUID().toString();
    private static final String PRODUCTION_EXPORT_FILE_ID_1 = UUID.randomUUID().toString();
    private static final String PRODUCTION_EXPORT_FILE_ID_2 = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        armRpoService = spy(ArmRpoService.class);
        armRpoClient = spy(ArmRpoClient.class);
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);

        getProductionOutputFilesService = new GetProductionOutputFilesServiceImpl(armRpoClient, armRpoService, armRpoUtil);

        armRpoHelperMocks = new ArmRpoHelperMocks(); // Mocks are set via the default constructor call
    }

    @AfterEach
    void afterEach() {
        armRpoHelperMocks.close();
    }

    @Test
    void getProductionOutputFiles_shouldSucceedAndReturnSingleItem_whenSuccessResponseIsReturnedFromArmWithSingularProductionExportFile() {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        var response = createProductionOutputFilesResponse(
            Collections.singletonList(createProductionExportFile(createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_1)))
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        List<String> productionOutputFiles = getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount);

        // Then
        assertEquals(1, productionOutputFiles.size());
        assertEquals(PRODUCTION_EXPORT_FILE_ID_1, productionOutputFiles.getFirst());

        // And verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getCompletedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getProductionOutputFiles_shouldSucceedAndReturnMultipleItems_whenSuccessResponseIsReturnedFromArmWithMultipleProductionExportFile() {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        var response = createProductionOutputFilesResponse(List.of(
                                                               createProductionExportFile(createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_1)),
                                                               createProductionExportFile(createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_2))
                                                           )
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        List<String> productionOutputFiles = getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount);

        // Then
        assertEquals(2, productionOutputFiles.size());
        assertThat(productionOutputFiles).containsExactlyInAnyOrder(PRODUCTION_EXPORT_FILE_ID_1, PRODUCTION_EXPORT_FILE_ID_2);

        // And verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getCompletedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getProductionOutputFiles_shouldSucceedAndReturnSingleItem_whenSuccessResponseIsReturnedFromArmWithMixtureOfPopulatedAndUnpopulatedExportFileIds(
        String productionExportFileId) {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        var response = createProductionOutputFilesResponse(List.of(
                                                               createProductionExportFile(createProductionExportFileDetail(productionExportFileId)),
                                                               createProductionExportFile(createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_1))
                                                           )
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        List<String> productionOutputFiles = getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount);

        // Then
        assertEquals(1, productionOutputFiles.size());
        assertEquals(PRODUCTION_EXPORT_FILE_ID_1, productionOutputFiles.getFirst());

        // And verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getCompletedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getProductionOutputFiles_shouldThrowException_whenExecutionDetailHasNoProductionId(String productionId) {
        // Given
        var armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setProductionId(productionId);

        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID))
            .thenReturn(armRpoExecutionDetailEntity);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("production id was blank for execution id"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getProductionOutputFiles_shouldThrowException_whenArmCallFails() {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenThrow(mock(FeignException.class));

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("API call failed"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getProductionOutputFiles_shouldThrowException_whenArmReturnsNoProductionExportFiles(
        List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles) {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        var response = createProductionOutputFilesResponse(productionExportFiles);

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("No production export files were returned"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void getProductionOutputFiles_shouldThrowException_whenArmReturnsNoProductionExportFileIds(String productionExportFileId) {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        var response = createProductionOutputFilesResponse(Collections.singletonList(
            createProductionExportFile(createProductionExportFileDetail(productionExportFileId)))
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("No production export file id's were returned"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getProductionOutputFiles_shouldReturnNonFailedProductions_whenOneFailedStatusIsReturnedFromArmWithMultipleProductionExportFile() {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail1 = createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_1);
        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail2 = createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_2);
        productionExportFileDetail2.setStatus(123);
        var response = createProductionOutputFilesResponse(List.of(
                                                               createProductionExportFile(productionExportFileDetail1),
                                                               createProductionExportFile(productionExportFileDetail2)
                                                           )
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        List<String> productionOutputFiles = getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount);

        // Then
        assertEquals(1, productionOutputFiles.size());
        assertEquals(PRODUCTION_EXPORT_FILE_ID_1, productionOutputFiles.getFirst());

        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to completed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getCompletedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getProductionOutputFiles_shouldThrowInProgressException_whenInProgressResponseIsReturnedFromArmWithMultipleProductionExportFile() {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail1 = createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_1);
        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail2 = createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_2);
        productionExportFileDetail2.setStatus(IN_PROGRESS_STATUS.getStatusCode());
        var response = createProductionOutputFilesResponse(List.of(
                                                               createProductionExportFile(productionExportFileDetail1),
                                                               createProductionExportFile(productionExportFileDetail2)
                                                           )
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoInProgressException armRpoInProgressException = assertThrows(ArmRpoInProgressException.class, () ->
            getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount));

        // Then
        assertThat(armRpoInProgressException.getMessage(),
                   containsString("RPO endpoint getProductionExportFileDetails is already in progress for execution id 1"));

        // And verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getProductionOutputFiles_shouldThrowException_whenAllStatusesAreInvalidWithMultipleProductionExportFile() {
        // Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail1 = createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_1);
        productionExportFileDetail1.setStatus(123);
        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail2 = createProductionExportFileDetail(PRODUCTION_EXPORT_FILE_ID_2);
        productionExportFileDetail2.setStatus(123);
        var response = createProductionOutputFilesResponse(List.of(
                                                               createProductionExportFile(productionExportFileDetail1),
                                                               createProductionExportFile(productionExportFileDetail2)
                                                           )
        );

        when(armRpoClient.getProductionOutputFiles(eq(TOKEN), any(ProductionOutputFilesRequest.class)))
            .thenReturn(response);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProductionOutputFilesService.getProductionOutputFiles(TOKEN, EXECUTION_ID, someUserAccount));

        // Then
        assertThat(armRpoException.getMessage(),
                   containsString("ARM getProductionOutputFiles: Production export files contain failures"));

        // And verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProductionOutputFilesRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    private ProductionOutputFilesResponse createProductionOutputFilesResponse(
        List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles) {
        var response = new ProductionOutputFilesResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setProductionExportFiles(productionExportFiles);
        return response;
    }

    private ProductionOutputFilesResponse.ProductionExportFile createProductionExportFile(
        ProductionOutputFilesResponse.ProductionExportFileDetail productionExportFileDetail) {
        var productionExportFile = new ProductionOutputFilesResponse.ProductionExportFile();
        productionExportFile.setProductionExportFileDetails(productionExportFileDetail);
        return productionExportFile;
    }

    private ProductionOutputFilesResponse.ProductionExportFileDetail createProductionExportFileDetail(String fileId) {
        var productionExportFileDetail = new ProductionOutputFilesResponse.ProductionExportFileDetail();
        productionExportFileDetail.setProductionExportFileId(fileId);
        productionExportFileDetail.setStatus(READY_STATUS.getStatusCode());
        return productionExportFileDetail;
    }

    private ArmRpoExecutionDetailEntity createInitialExecutionDetailEntityAndSetMock() {
        var armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);

        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID))
            .thenReturn(armRpoExecutionDetailEntity);

        return armRpoExecutionDetailEntity;
    }

}