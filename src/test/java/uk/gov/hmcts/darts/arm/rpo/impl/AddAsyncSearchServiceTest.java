package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.rpo.AddAsyncSearchService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddAsyncSearchServiceTest {

    private ArmRpoService armRpoService;
    private ArmRpoClient armRpoClient;
    private ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private AddAsyncSearchService addAsyncSearchService;

    private ArgumentCaptor<ArmRpoExecutionDetailEntity> executionDetailCaptor;
    private ArgumentCaptor<String> requestCaptor;
    private ArmRpoHelperMocks armRpoHelperMocks;

    private static final Integer EXECUTION_ID = 1;
    private static final int RPO_CSV_START_HOUR = 25;
    private static final int RPO_CSV_END_HOUR = 49;
    private static final String SEARCH_ID = "some search id";
    private static final String MATTER_ID = "some matter id";
    private static final String ENTITLEMENT_ID = "some entitlement id";
    private static final String INDEX_ID = "some index id";
    private static final String SORTING_FIELD = "some sorting field";
    private static final String TOKEN = "some token";

    @BeforeEach
    void addAsyncSearch() {
        armRpoService = spy(ArmRpoService.class);
        armRpoClient = spy(ArmRpoClient.class);
        armAutomatedTaskRepository = mock(ArmAutomatedTaskRepository.class);
        var currentTimeHelper = mock(CurrentTimeHelper.class);
        
        executionDetailCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);
        requestCaptor = ArgumentCaptor.forClass(String.class);

        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);

        addAsyncSearchService = new AddAsyncSearchServiceImpl(armRpoClient, armRpoService, armRpoUtil,
                                                              armAutomatedTaskRepository, currentTimeHelper);

        armRpoHelperMocks = new ArmRpoHelperMocks(); // Mocks are set via the default constructor call

        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.parse("2024-01-01T12:00:00Z"));
    }

    @AfterEach
    void afterEach() {
        armRpoHelperMocks.close();
    }

    @Test
    void addAsyncSearch_shouldSucceedWhenASuccessResponseIsObtainedFromArm() {
        //  Given
        createArmAutomatedTaskEntityAndSetMock();
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        createSearchResponseAndSetMock(SEARCH_ID);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        String searchName = addAsyncSearchService.addAsyncSearch(TOKEN, EXECUTION_ID, someUserAccount);

        // Then
        assertThat(searchName, Matchers.matchesPattern("DARTS_RPO_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}"));

        // And verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getAddAsyncSearchRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify the expected request data has been created
        verify(armRpoClient).addAsyncSearch(eq(TOKEN), requestCaptor.capture());

        String jsonRequest = requestCaptor.getValue();

        assertEquals("DARTS_RPO_2024_01_01_12_00_00", parse(jsonRequest)
            .read("$.name", String.class));

        assertEquals("DARTS_RPO_2024_01_01_12_00_00", parse(jsonRequest)
            .read("$.searchName", String.class));

        assertEquals(MATTER_ID, parse(jsonRequest)
            .read("$.matterId", String.class));

        assertEquals(ENTITLEMENT_ID, parse(jsonRequest)
            .read("$.entitlementId", String.class));

        assertEquals(INDEX_ID, parse(jsonRequest)
            .read("$.indexId", String.class));

        assertEquals(SORTING_FIELD, parse(jsonRequest)
            .read("$.sortingField", String.class));

        assertEquals("2023-12-30T11:00:00Z", parse(jsonRequest)
            .read("$.queryTree.children[1].field.value[0]", String.class));

        assertEquals("2023-12-31T11:00:00Z", parse(jsonRequest)
            .read("$.queryTree.children[1].field.value[1]", String.class));

        // And verify execution detail status moves to completed as the final operation
        verify(armRpoService).updateArmRpoStatus(executionDetailCaptor.capture(),
                                                 eq(armRpoHelperMocks.getCompletedRpoStatus()),
                                                 eq(someUserAccount));
        verifyNoMoreInteractions(armRpoService);

        // And verify the search id was set
        assertEquals(SEARCH_ID, executionDetailCaptor.getValue().getSearchId());
    }

    @Test
    void addAsyncSearch_shouldThrowException_whenAutomatedTaskIsNotFound() {
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            addAsyncSearchService.addAsyncSearch(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("Automated task not found: ProcessE2EArmRpoPending"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getAddAsyncSearchRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void addAsyncSearch_shouldThrowException_whenArmRequestCannotBeConstructed() {
        // Given
        createArmAutomatedTaskEntityAndSetMock();

        var armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setMatterId(null);

        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID))
            .thenReturn(armRpoExecutionDetailEntity);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        String exceptionMessage = assertThrows(ArmRpoException.class, () ->
            addAsyncSearchService.addAsyncSearch(TOKEN, EXECUTION_ID, someUserAccount))
            .getMessage();
        assertThat(exceptionMessage, containsString("Could not construct API request"));
        assertThat(exceptionMessage, containsString("matterId is marked non-null but is null"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getAddAsyncSearchRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void addAsyncSearch_shouldThrowException_whenArmCallFails() {
        createArmAutomatedTaskEntityAndSetMock();
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();

        when(armRpoClient.addAsyncSearch(eq(TOKEN), anyString()))
            .thenThrow(mock(FeignException.class));

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            addAsyncSearchService.addAsyncSearch(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("API call failed"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getAddAsyncSearchRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void addAsyncSearch_shouldThrowException_whenArmReturnsNullSearchId() {
        createArmAutomatedTaskEntityAndSetMock();
        var armRpoExecutionDetailEntity = createInitialExecutionDetailEntityAndSetMock();
        createSearchResponseAndSetMock(null);

        UserAccountEntity someUserAccount = new UserAccountEntity();

        // When
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            addAsyncSearchService.addAsyncSearch(TOKEN, EXECUTION_ID, someUserAccount));
        assertThat(armRpoException.getMessage(), containsString("The obtained search id was empty"));

        // Then verify execution detail state moves to in progress
        verify(armRpoService).updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                         armRpoHelperMocks.getAddAsyncSearchRpoState(),
                                                         armRpoHelperMocks.getInProgressRpoStatus(),
                                                         someUserAccount);

        // And verify execution detail status moves to failed as the final operation
        verify(armRpoService).updateArmRpoStatus(armRpoExecutionDetailEntity,
                                                 armRpoHelperMocks.getFailedRpoStatus(),
                                                 someUserAccount);
        verifyNoMoreInteractions(armRpoService);
    }

    private void createSearchResponseAndSetMock(String searchId) {
        var response = new ArmAsyncSearchResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setSearchId(searchId);

        when(armRpoClient.addAsyncSearch(eq(TOKEN), anyString()))
            .thenReturn(response);
    }

    private ArmRpoExecutionDetailEntity createInitialExecutionDetailEntityAndSetMock() {
        var armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        armRpoExecutionDetailEntity.setEntitlementId(ENTITLEMENT_ID);
        armRpoExecutionDetailEntity.setIndexId(INDEX_ID);
        armRpoExecutionDetailEntity.setSortingField(SORTING_FIELD);

        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID))
            .thenReturn(armRpoExecutionDetailEntity);

        return armRpoExecutionDetailEntity;
    }

    private ArmAutomatedTaskEntity createArmAutomatedTaskEntityAndSetMock() {
        var armAutomatedTaskEntity = new ArmAutomatedTaskEntity();
        armAutomatedTaskEntity.setRpoCsvStartHour(RPO_CSV_START_HOUR);
        armAutomatedTaskEntity.setRpoCsvEndHour(RPO_CSV_END_HOUR);

        when(armAutomatedTaskRepository.findByAutomatedTask_taskName(any()))
            .thenReturn(Optional.of(armAutomatedTaskEntity));

        return armAutomatedTaskEntity;
    }

}