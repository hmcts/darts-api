package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddAsyncSearchServiceIntTest extends PostgresIntegrationBase {

    @MockitoBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private AddAsyncSearchService addAsyncSearchService;

    private static final String TOKEN = "some token";
    private static final String SEARCH_ID = "some search id";
    private static final String MATTER_ID = "some matter id";
    private static final String ENTITLEMENT_ID = "some entitlement id";
    private static final String INDEX_ID = "some index id";
    private static final String SORTING_FIELD = "some sorting field";

    @Test
    void addAsyncSearch_shouldSucceedWhenSuccessResponseIsObtainedFromArmUsingZeroDateTimeMillis() {
        // Given
        createSearchResponseAndSetMock();

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var executionDetailEntity = createExecutionDetailEntity(userAccount);
        executionDetailEntity.setMatterId(MATTER_ID);
        executionDetailEntity.setEntitlementId(ENTITLEMENT_ID);
        executionDetailEntity.setIndexId(INDEX_ID);
        executionDetailEntity.setSortingField(SORTING_FIELD);
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);
        executionDetailEntity.setCreatedDateTime(OffsetDateTime.parse("2024-07-31T11:29:00.00000Z"));
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);

        Integer executionId = executionDetailEntity.getId();

        // When
        addAsyncSearchService.addAsyncSearch(TOKEN, executionId, userAccount);

        // Then
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

        verify(armRpoClient).addAsyncSearch(tokenCaptor.capture(), requestCaptor.capture());

        assertEquals(TOKEN, tokenCaptor.getValue());
        assertThat(requestCaptor.getValue(), containsString(MATTER_ID));
        assertThat(requestCaptor.getValue(), containsString(ENTITLEMENT_ID));
        assertThat(requestCaptor.getValue(), containsString(INDEX_ID));
        assertThat(requestCaptor.getValue(), containsString(SORTING_FIELD));
        assertThat(requestCaptor.getValue(), containsString("DARTS_RPO_2024_07_31_11_29_00"));
        assertThat(requestCaptor.getValue(), containsString("2024-07-29T10:29:00Z"));
        assertThat(requestCaptor.getValue(), containsString("2024-07-30T10:29:00Z"));

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.ADD_ASYNC_SEARCH.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), executionDetailEntity.getArmRpoStatus().getId());
        assertEquals(SEARCH_ID, executionDetailEntity.getSearchId());

    }

    @Test
    void addAsyncSearch_shouldSucceedWhenSuccessResponseIsObtainedFromArmUsingNonZeroDateTime() {
        // Given
        createSearchResponseAndSetMock();

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var executionDetailEntity = createExecutionDetailEntity(userAccount);
        executionDetailEntity.setMatterId(MATTER_ID);
        executionDetailEntity.setEntitlementId(ENTITLEMENT_ID);
        executionDetailEntity.setIndexId(INDEX_ID);
        executionDetailEntity.setSortingField(SORTING_FIELD);
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);
        executionDetailEntity.setCreatedDateTime(OffsetDateTime.parse("2024-07-31T12:34:56.101701Z"));
        executionDetailEntity = dartsPersistence.save(executionDetailEntity);

        Integer executionId = executionDetailEntity.getId();

        // When
        addAsyncSearchService.addAsyncSearch(TOKEN, executionId, userAccount);

        // Then
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

        verify(armRpoClient).addAsyncSearch(tokenCaptor.capture(), requestCaptor.capture());

        assertEquals(TOKEN, tokenCaptor.getValue());
        assertThat(requestCaptor.getValue(), containsString(MATTER_ID));
        assertThat(requestCaptor.getValue(), containsString(ENTITLEMENT_ID));
        assertThat(requestCaptor.getValue(), containsString(INDEX_ID));
        assertThat(requestCaptor.getValue(), containsString(SORTING_FIELD));
        assertThat(requestCaptor.getValue(), containsString("DARTS_RPO_2024_07_31_12_34_00"));
        assertThat(requestCaptor.getValue(), containsString("2024-07-29T11:34:00Z"));
        assertThat(requestCaptor.getValue(), containsString("2024-07-30T11:34:00Z"));

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.ADD_ASYNC_SEARCH.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), executionDetailEntity.getArmRpoStatus().getId());
        assertEquals(SEARCH_ID, executionDetailEntity.getSearchId());

    }

    @Test
    void addAsyncSearch_shouldThrowExceptionWhenArmRequestCannotBeConstructed() {
        // Given
        createSearchResponseAndSetMock();

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var executionDetailEntity = dartsPersistence.save(createExecutionDetailEntity(userAccount));
        Integer executionId = executionDetailEntity.getId();

        // When
        String exceptionMessage = assertThrows(ArmRpoException.class, () ->
            addAsyncSearchService.addAsyncSearch(TOKEN, executionId, userAccount))
            .getMessage();

        // Then
        assertThat(exceptionMessage, containsString("Could not construct API request"));
        assertThat(exceptionMessage, containsString("matterId is marked non-null but is null"));

        executionDetailEntity = dartsPersistence.getArmRpoExecutionDetailRepository().findById(executionId)
            .orElseThrow();
        assertEquals(ArmRpoStateEnum.ADD_ASYNC_SEARCH.getId(), executionDetailEntity.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), executionDetailEntity.getArmRpoStatus().getId());
    }

    private ArmRpoExecutionDetailEntity createExecutionDetailEntity(UserAccountEntity userAccount) {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        return armRpoExecutionDetailEntity;
    }

    private void createSearchResponseAndSetMock() {
        var response = new ArmAsyncSearchResponse();
        response.setStatus(200);
        response.setIsError(false);
        response.setSearchId(SEARCH_ID);

        when(armRpoClient.addAsyncSearch(eq(TOKEN), anyString()))
            .thenReturn(response);
    }

}
