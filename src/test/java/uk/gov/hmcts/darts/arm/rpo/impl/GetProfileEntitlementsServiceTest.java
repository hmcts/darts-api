package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.rpo.GetProfileEntitlementsService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfileEntitlementsServiceTest {

    private GetProfileEntitlementsService getProfileEntitlementsService;

    private ArmRpoService armRpoService;
    private ArmRpoClient armRpoClient;

    private ArmRpoHelperMocks armRpoHelperMocks;
    private ArgumentCaptor<ArmRpoExecutionDetailEntity> executionDetailCaptor;

    private static final Integer EXECUTION_ID = 1;
    private static final String TOKEN = "some token";
    private static final String ENTITLEMENT_NAME = "some entitlement name";

    @BeforeEach
    void beforeEach() {
        armRpoService = spy(ArmRpoService.class);
        armRpoClient = mock(ArmRpoClient.class);
        executionDetailCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        
        armRpoHelperMocks = new ArmRpoHelperMocks(); // Mocks are set via the default constructor call

        ArmApiConfigurationProperties armApiConfigurationProperties = new ArmApiConfigurationProperties();
        armApiConfigurationProperties.setArmServiceEntitlement(ENTITLEMENT_NAME);
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);

        getProfileEntitlementsService = new GetProfileEntitlementsServiceImpl(armRpoClient, armRpoService, armRpoUtil, armApiConfigurationProperties);
    }

    @AfterEach
    void afterEach() {
        armRpoHelperMocks.close();
    }

    @Test
    void getProfileEntitlements_shouldSucceed_whenAResponseIsObtainedFromArmThatContainsAMatchingEntitlement() {
        //  Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        var profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName(ENTITLEMENT_NAME);
        profileEntitlement.setEntitlementId("some entitlement id");
        createEntitlementResponseAndSetMock(Collections.singletonList(profileEntitlement), emptyRpoRequest);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        getProfileEntitlementsService.getProfileEntitlements(TOKEN, EXECUTION_ID, someUserAccount);

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProfileEntitlementsRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to completed as the final operation
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(armRpoHelperMocks.getCompletedRpoStatus()),
                                                 eq(someUserAccount));
        verifyNoMoreInteractions(armRpoService);

        // And verify the entitlement id was set
        assertEquals("some entitlement id", executionDetailCaptor.getValue().getEntitlementId());
    }

    @Test
    void getProfileEntitlements_shouldThrowException_whenArmCallFails() {
        //  Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armRpoClient.getProfileEntitlementResponse(TOKEN, emptyRpoRequest))
            .thenThrow(mock(FeignException.class));

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProfileEntitlementsService.getProfileEntitlements(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("API call failed"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProfileEntitlementsRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getProfileEntitlements_shouldThrowException_whenEntitlementsAreNullOrEmpty(List<ProfileEntitlementResponse.ProfileEntitlement> entitlements) {
        //  Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        createEntitlementResponseAndSetMock(entitlements, emptyRpoRequest);

        var someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProfileEntitlementsService.getProfileEntitlements(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("No entitlements were returned"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProfileEntitlementsRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getProfileEntitlements_shouldThrowException_whenNoMatchingEntitlementIsReturned() {
        //  Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        var profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName("some lone name that does not match the configured entitlement name");
        createEntitlementResponseAndSetMock(Collections.singletonList(profileEntitlement), emptyRpoRequest);

        var someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProfileEntitlementsService.getProfileEntitlements(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("ARM getProfileEntitlements: No matching entitlements 'some entitlement name' were returned"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProfileEntitlementsRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getProfileEntitlements_shouldThrowException_whenEntitlementIsReturnedButContainsANullOrEmptyEntitlementId(String entitlementId) {
        //  Given
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        var profileEntitlement = new ProfileEntitlementResponse.ProfileEntitlement();
        profileEntitlement.setName(ENTITLEMENT_NAME);
        profileEntitlement.setEntitlementId(entitlementId);
        createEntitlementResponseAndSetMock(Collections.singletonList(profileEntitlement), emptyRpoRequest);

        var someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            getProfileEntitlementsService.getProfileEntitlements(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("The obtained entitlement id was empty"));


        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getGetProfileEntitlementsRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    private ArmRpoExecutionDetailEntity createInitialExecutionDetailEntityAndSetMock() {
        var armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);

        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID))
            .thenReturn(armRpoExecutionDetailEntity);

        return armRpoExecutionDetailEntity;
    }

    private void createEntitlementResponseAndSetMock(List<ProfileEntitlementResponse.ProfileEntitlement> profileEntitlements, EmptyRpoRequest emptyRpoRequest) {
        var response = new ProfileEntitlementResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setEntitlements(profileEntitlements);
        when(armRpoClient.getProfileEntitlementResponse(TOKEN, emptyRpoRequest))
            .thenReturn(response);
    }

}