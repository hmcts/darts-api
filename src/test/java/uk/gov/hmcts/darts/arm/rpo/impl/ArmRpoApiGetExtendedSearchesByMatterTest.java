package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

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
class ArmRpoApiGetExtendedSearchesByMatterTest {

    private static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    private static final String SEARCH_ID = "8271f101-8c14-4c41-8865-edc5d8baed99";

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setSearchId(SEARCH_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
    }

    @Test
    void getExtendedSearchesByMatter_Success() {
        // given
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        extendedSearchesByMatterResponse.setStatus(200);
        extendedSearchesByMatterResponse.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setSearchId(SEARCH_ID);
        search.setTotalCount(4);
        search.setName(PRODUCTION_NAME);
        search.setIsSaved(true);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        extendedSearchesByMatterResponse.setSearches(List.of(searchDetail));

        armRpoExecutionDetailEntity.setMatterId("1");

        when(armRpoClient.getExtendedSearchesByMatter(anyString(), any())).thenReturn(extendedSearchesByMatterResponse);

        // when
        String result = armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount);

        // then
        assertThat(result, containsString(PRODUCTION_NAME));
        verify(armRpoService).updateArmRpoStateAndStatus(any(ArmRpoExecutionDetailEntity.class),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any(UserAccountEntity.class));
        verify(armRpoService).updateArmRpoStatus(any(ArmRpoExecutionDetailEntity.class), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()),
                                                 any(UserAccountEntity.class));
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WhenIsSavedFalse() {
        // given
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        extendedSearchesByMatterResponse.setStatus(200);
        extendedSearchesByMatterResponse.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setSearchId(SEARCH_ID);
        search.setTotalCount(4);
        search.setName(PRODUCTION_NAME);
        search.setIsSaved(false);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        extendedSearchesByMatterResponse.setSearches(List.of(searchDetail));

        armRpoExecutionDetailEntity.setMatterId("1");

        when(armRpoClient.getExtendedSearchesByMatter(anyString(), any())).thenReturn(extendedSearchesByMatterResponse);

        // when
        ArmRpoInProgressException armRpoInProgressException = assertThrows(
            ArmRpoInProgressException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount));

        // then
        assertThat(armRpoInProgressException.getMessage(), containsString(
            "RPO endpoint extendedSearchesByMatterResponse is already in progress for execution id 1"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(ArmRpoExecutionDetailEntity.class),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any(UserAccountEntity.class));
        verifyNoMoreInteractions(armRpoService);

    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WhenClientThrowsFeignException() {
        // given
        when(armRpoClient.getExtendedSearchesByMatter(anyString(), anyString())).thenThrow(FeignException.class);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO getExtendedSearchesByMatter: Unable to get ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(ArmRpoExecutionDetailEntity.class),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any(UserAccountEntity.class));
        verify(armRpoService).updateArmRpoStatus(any(ArmRpoExecutionDetailEntity.class), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
                                                 any(UserAccountEntity.class));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WithNullResponse() {
        // given
        when(armRpoClient.getExtendedSearchesByMatter(anyString(), anyString())).thenReturn(null);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO getExtendedSearchesByMatter: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(ArmRpoExecutionDetailEntity.class),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any(UserAccountEntity.class));
        verify(armRpoService).updateArmRpoStatus(any(ArmRpoExecutionDetailEntity.class), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
                                                 any(UserAccountEntity.class));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WithEmptyResponse() {
        // given
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        when(armRpoClient.getExtendedSearchesByMatter(anyString(), anyString())).thenReturn(extendedSearchesByMatterResponse);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO getExtendedSearchesByMatter: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WithMissingTotalCount() {
        // given
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        extendedSearchesByMatterResponse.setStatus(200);
        extendedSearchesByMatterResponse.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setSearchId(SEARCH_ID);
        search.setName(PRODUCTION_NAME);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        extendedSearchesByMatterResponse.setSearches(List.of(searchDetail));
        when(armRpoClient.getExtendedSearchesByMatter(anyString(), anyString())).thenReturn(extendedSearchesByMatterResponse);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO getExtendedSearchesByMatter: extendedSearchesByMatterResponse search data is missing for searchId"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(ArmRpoExecutionDetailEntity.class),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(ArmRpoExecutionDetailEntity.class), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
                                                 any(UserAccountEntity.class));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WithMissingSearchName() {
        // given
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        extendedSearchesByMatterResponse.setStatus(200);
        extendedSearchesByMatterResponse.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setSearchId(SEARCH_ID);
        search.setTotalCount(4);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        extendedSearchesByMatterResponse.setSearches(List.of(searchDetail));
        when(armRpoClient.getExtendedSearchesByMatter(anyString(), anyString())).thenReturn(extendedSearchesByMatterResponse);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO getExtendedSearchesByMatter: extendedSearchesByMatterResponse search data is missing for searchId"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(ArmRpoExecutionDetailEntity.class),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedSearchesByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(ArmRpoExecutionDetailEntity.class), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
                                                 any(UserAccountEntity.class));
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}