package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceImpl;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveBackgroundSearchServiceTest {

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private SaveBackgroundSearchServiceImpl saveBackgroundSearchService;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();

    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);

        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);
        saveBackgroundSearchService = new SaveBackgroundSearchServiceImpl(armClientService, armRpoService, armRpoUtil);
    }

    @Test
    void saveBackgroundSearchReturnsSuccess() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        saveBackgroundSearchResponse.setIsError(false);
        when(armRpoClient.saveBackgroundSearch(anyString(), any())).thenReturn(saveBackgroundSearchResponse);

        // when
        saveBackgroundSearchService.saveBackgroundSearch("token", 1, "searchName", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearchReturnsInvalidStatus() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(400);
        saveBackgroundSearchResponse.setIsError(true);
        when(armRpoClient.saveBackgroundSearch(anyString(), any())).thenReturn(saveBackgroundSearchResponse);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API failed with status - 400 BAD_REQUEST and response"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void saveBackgroundSearchThrowsFeignException() {
        // given
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class)))
            .thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: Unable to save background search"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getStorageAccountsReturnsNullResponse() {
        // given
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid - null"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getStorageAccountsReturnsNullStatusHttpResponse() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setIsError(true);
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getStorageAccountsReturnsNullIsErrorResponse() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(200);
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getStorageAccountsReturnsInvalidHttpStatusResponse() {
        // given
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = new SaveBackgroundSearchResponse();
        saveBackgroundSearchResponse.setStatus(-1);
        when(armRpoClient.saveBackgroundSearch(anyString(), any(SaveBackgroundSearchRequest.class))).thenReturn(null);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () -> saveBackgroundSearchService.saveBackgroundSearch(
            "token", 1, "searchName", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM save background search: ARM RPO API response is invalid"));

        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getSaveBackgroundSearchRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }

}