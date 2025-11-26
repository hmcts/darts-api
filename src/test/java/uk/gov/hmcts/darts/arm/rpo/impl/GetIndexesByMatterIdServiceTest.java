package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
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
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceImpl;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.nio.charset.StandardCharsets;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.CloseResource"})
class GetIndexesByMatterIdServiceTest {

    private static final Integer EXECUTION_ID = 1;
    private static final String BEARER_TOKEN = "token";

    @Mock
    private ArmRpoClient armRpoClient;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private ArmRpoService armRpoService;

    private ArmRpoUtil armRpoUtil;

    private GetIndexesByMatterIdServiceImpl getIndexesByMatterIdService;

    @Captor
    private ArgumentCaptor<ArmRpoExecutionDetailEntity> armRpoExecutionDetailEntityArgumentCaptor;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccount;

    private ArmRpoHelperMocks armRpoHelperMocks;

    @BeforeEach
    void setUp() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        userAccount = new UserAccountEntity();
        armRpoExecutionDetailEntityArgumentCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        armRpoUtil = spy(new ArmRpoUtil(armRpoService, armApiService));
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);

        getIndexesByMatterIdService = new GetIndexesByMatterIdServiceImpl(armClientService, armRpoService, armRpoUtil);
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("indexId", armRpoExecutionDetailEntityArgumentCaptor.getValue().getIndexId());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ThrowsFeignException() {
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_WithNullResponse() {
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(anyString(), any(IndexesByMatterIdRequest.class))).thenReturn(null);

        ArmRpoException armRpoException = assertThrows(ArmRpoException.class,
                                                       () -> getIndexesByMatterIdService.getIndexesByMatterId("token", 1, "matterId", userAccount));

        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_WithEmptyIndexes() {
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ShouldRetryOnUnauthorised_WhenResponseIsValid() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/getIndexesByMatterId", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(401)
            .reason("Unauthorised")
            .build();
        FeignException feign401 = FeignException.errorStatus("getIndexesByMatterId", response);
        when(armRpoClient.getIndexesByMatterId(eq(BEARER_TOKEN), any(IndexesByMatterIdRequest.class))).thenThrow(feign401);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken(anyString());

        IndexesByMatterIdResponse indexesByMatterIdResponse = new IndexesByMatterIdResponse();
        indexesByMatterIdResponse.setStatus(200);
        indexesByMatterIdResponse.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId("indexId");
        index.setIndexDetails(indexDetails);
        indexesByMatterIdResponse.setIndexes(List.of(index));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(eq("Bearer refreshed"), any(IndexesByMatterIdRequest.class))).thenReturn(indexesByMatterIdResponse);

        // when
        getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("indexId", armRpoExecutionDetailEntityArgumentCaptor.getValue().getIndexId());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getCompletedRpoStatus()), eq(userAccount));
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getIndexesByMatterId_ShouldRetryOnForbidden_WhenResponseIsValid() {
        // given
        Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.POST, "/getIndexesByMatterId", java.util.Map.of(), null,
                                    StandardCharsets.UTF_8, null))
            .status(403)
            .reason("Forbidden")
            .build();
        FeignException feign403 = FeignException.errorStatus("getIndexesByMatterId", response);
        when(armRpoClient.getIndexesByMatterId(eq(BEARER_TOKEN), any(IndexesByMatterIdRequest.class))).thenThrow(feign403);

        // armRpoUtil should be asked for a new token
        doReturn("Bearer refreshed").when(armRpoUtil).retryGetBearerToken("getIndexesByMatterId");

        IndexesByMatterIdResponse indexesByMatterIdResponse = new IndexesByMatterIdResponse();
        indexesByMatterIdResponse.setStatus(200);
        indexesByMatterIdResponse.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId("indexId");
        index.setIndexDetails(indexDetails);
        indexesByMatterIdResponse.setIndexes(List.of(index));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);
        when(armRpoClient.getIndexesByMatterId(eq("Bearer refreshed"), any(IndexesByMatterIdRequest.class))).thenReturn(indexesByMatterIdResponse);

        // when
        getIndexesByMatterIdService.getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, "matterId", userAccount);

        // then
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntityArgumentCaptor.capture(),
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("indexId", armRpoExecutionDetailEntityArgumentCaptor.getValue().getIndexId());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getCompletedRpoStatus()), eq(userAccount));
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        assertEquals("indexId", armRpoExecutionDetailEntityArgumentCaptor.getValue().getIndexId());
        verify(armRpoService).updateArmRpoStatus(eq(armRpoExecutionDetailEntity), eq(armRpoHelperMocks.getCompletedRpoStatus()), eq(userAccount));
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
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
                                                         eq(armRpoHelperMocks.getGetIndexesByMatterIdRpoState()),
                                                         eq(armRpoHelperMocks.getInProgressRpoStatus()),
                                                         eq(userAccount));
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoHelperMocks.getFailedRpoStatus(), userAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterEach
    void close() {
        armRpoHelperMocks.close();
    }
}