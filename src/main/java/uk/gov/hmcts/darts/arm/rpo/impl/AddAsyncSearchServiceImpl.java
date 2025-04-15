package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.component.impl.AddAsyncSearchRequestGenerator;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.AddAsyncSearchService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@AllArgsConstructor
@Slf4j
public class AddAsyncSearchServiceImpl implements AddAsyncSearchService {

    private static final String ADD_ASYNC_SEARCH_RELATED_TASK_NAME = "ProcessE2EArmRpoPending";

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public String addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount) {

        log.info("addAsyncSearch called with executionId: {}", executionId);
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.addAsyncSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        OffsetDateTime now = currentTimeHelper.currentOffsetDateTime();
        String searchName = "DARTS_RPO_%s".formatted(
            now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
        );

        final StringBuilder exceptionMessageBuilder = new StringBuilder(99).append("ARM addAsyncSearch: ");
        ArmAutomatedTaskEntity armAutomatedTaskEntity = armAutomatedTaskRepository.findByAutomatedTask_taskName(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
            .orElseThrow(() -> armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("Automated task not found: ")
                                                                              .append(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
                                                                              .toString(),
                                                                          executionDetail, userAccount));

        AddAsyncSearchRequestGenerator requestGenerator;
        try {
            requestGenerator = createAddAsyncSearchRequestGenerator(searchName, executionDetail, armAutomatedTaskEntity, now);
        } catch (Exception e) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append(ArmRpoUtil.COULD_NOT_CONSTRUCT_API_REQUEST)
                                                                 .append(e)
                                                                 .toString(),
                                                             executionDetail, userAccount);
        }

        ArmAsyncSearchResponse armAsyncSearchResponse;
        try {
            armAsyncSearchResponse = armRpoClient.addAsyncSearch(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ").append(e).toString(),
                                                             executionDetail, userAccount);
        }
        log.info("ARM RPO Response - ArmAsyncSearchResponse: {}", armAsyncSearchResponse);
        return processAddAsyncSearch(userAccount, armAsyncSearchResponse, exceptionMessageBuilder, executionDetail, searchName);
    }

    private String processAddAsyncSearch(UserAccountEntity userAccount, ArmAsyncSearchResponse armAsyncSearchResponse, StringBuilder exceptionMessageBuilder,
                                         ArmRpoExecutionDetailEntity executionDetail, String searchName) {
        armRpoUtil.handleResponseStatus(userAccount, armAsyncSearchResponse, exceptionMessageBuilder, executionDetail);

        String searchId = armAsyncSearchResponse.getSearchId();
        if (searchId == null) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("The obtained search id was empty").toString(),
                                                             executionDetail, userAccount);
        }

        executionDetail.setSearchId(searchId);
        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);

        return searchName;
    }

    private AddAsyncSearchRequestGenerator createAddAsyncSearchRequestGenerator(String searchName,
                                                                                ArmRpoExecutionDetailEntity executionDetail,
                                                                                ArmAutomatedTaskEntity armAutomatedTaskEntity,
                                                                                OffsetDateTime now) {
        return AddAsyncSearchRequestGenerator.builder()
            .name(searchName)
            .searchName(searchName)
            .matterId(executionDetail.getMatterId())
            .entitlementId(executionDetail.getEntitlementId())
            .indexId(executionDetail.getIndexId())
            .sortingField(executionDetail.getSortingField())
            .startTime(now.minusHours(armAutomatedTaskEntity.getRpoCsvEndHour()))
            .endTime(now.minusHours(armAutomatedTaskEntity.getRpoCsvStartHour()))
            .build();
    }

}
