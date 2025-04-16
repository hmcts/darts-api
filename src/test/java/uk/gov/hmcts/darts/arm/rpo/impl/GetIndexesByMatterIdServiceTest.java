package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetIndexesByMatterIdServiceTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private GetIndexesByMatterIdServiceImpl getIndexesByMatterIdService;

    @Captor
    private ArgumentCaptor<ArmRpoExecutionDetailEntity> armRpoExecutionDetailEntityArgumentCaptor;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;

    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();


    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        userAccount = new UserAccountEntity();
        armRpoExecutionDetailEntityArgumentCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);
        getIndexesByMatterIdService = new GetIndexesByMatterIdServiceImpl(armRpoClient, armRpoService, armRpoUtil);
    }

    @Test
    void getIndexesByMatterId_Success() {
        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId("indexId");
        index.setIndexDetails(indexDetails);
        response.setIndexes(List.of(index));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        // when
        getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("indexId", armRpoExecutionDetailEntityArgumentCaptor.getValue().getIndexId());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterIdThrowsFeignException() {
        // given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenThrow(FeignException.class);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class,
                                                       () -> getIndexesByMatterIdService.getIndexesByMatterId("token", 1, "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to get ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterIdWithNullResponse() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(null);

        ArmRpoException armRpoException = assertThrows(ArmRpoException.class,
                                                       () -> getIndexesByMatterIdService.getIndexesByMatterId("token", 1, "matterId", userAccount));

        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterIdWithEmptyIndexes() {
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setIndexes(Collections.emptyList());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        ArmRpoException armRpoException = assertThrows(ArmRpoException.class,
                                                       () -> getIndexesByMatterIdService.getIndexesByMatterId("token", 1, "matterId", userAccount));

        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ThrowsException_WithMissingIndexDetails() {
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        response.setIndexes(List.of(index));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        ArmRpoException armRpoException = assertThrows(ArmRpoException.class,
                                                       () -> getIndexesByMatterIdService.getIndexesByMatterId("token", 1, "matterId", userAccount));

        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_Success_WithMultipleIndexes() {
        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index1 = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails1 = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails1.setIndexId("indexId");
        index1.setIndexDetails(indexDetails1);

        IndexesByMatterIdResponse.Index index2 = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails2 = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails2.setIndexId("indexId 2");
        index2.setIndexDetails(indexDetails2);

        response.setIndexes(List.of(index1, index2));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        // when
        getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("indexId", armRpoExecutionDetailEntityArgumentCaptor.getValue().getIndexId());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ThrowsException_WithNullIndexId() {
        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId(null);
        index.setIndexDetails(indexDetails);
        response.setIndexes(List.of(index));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ThrowsException_WithNullIndexDetails() {
        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        index.setIndexDetails(null);
        response.setIndexes(List.of(index));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ThrowsException_WithEmptyIndexes() {
        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setIndexes(Collections.emptyList());

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ThrowsException_WithNullIndexes() {
        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setIndexes(null);

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(response);

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetIndexesByMatterIdRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}