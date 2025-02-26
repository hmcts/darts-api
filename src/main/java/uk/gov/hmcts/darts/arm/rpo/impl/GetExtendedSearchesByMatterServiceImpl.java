package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.component.impl.GetExtendedSearchesByMatterRequestGenerator;
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetExtendedSearchesByMatterService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@AllArgsConstructor
@Slf4j
public class GetExtendedSearchesByMatterServiceImpl implements GetExtendedSearchesByMatterService {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

    @Override
    public String getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.debug("getExtendedSearchesByMatter called with executionId: {}", executionId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getExtendedSearchesByMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(153).append("Failure during ARM RPO getExtendedSearchesByMatter: ");
        GetExtendedSearchesByMatterRequestGenerator requestGenerator;
        try {
            requestGenerator = createExtendedSearchesByMatterRequestGenerator(armRpoExecutionDetailEntity.getMatterId());
        } catch (Exception e) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(armRpoUtil.COULD_NOT_CONSTRUCT_API_REQUEST).append(e)
                                                                 .toString(), armRpoExecutionDetailEntity, userAccount);
        }

        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse;
        try {
            extendedSearchesByMatterResponse = armRpoClient.getExtendedSearchesByMatter(bearerToken, requestGenerator.getJsonRequest());
        } catch (FeignException e) {
            log.error(errorMessage.append("Unable to get ARM RPO response {}").append(e).toString(), e);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - ExtendedSearchesByMatterResponse: {}", extendedSearchesByMatterResponse);
        return processExtendedSearchesByMatterResponse(executionId, userAccount, extendedSearchesByMatterResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private String processExtendedSearchesByMatterResponse(Integer executionId, UserAccountEntity userAccount,
                                                           ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse,
                                                           StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        armRpoUtil.handleResponseStatus(userAccount, extendedSearchesByMatterResponse, errorMessage, armRpoExecutionDetailEntity);

        if (isNull(extendedSearchesByMatterResponse.getSearches())
            || CollectionUtils.isEmpty(extendedSearchesByMatterResponse.getSearches())) {

            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Search data is missing").toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }

        String searchId = armRpoExecutionDetailEntity.getSearchId();

        ExtendedSearchesByMatterResponse.SearchDetail searchDetailMatch = null;
        for (ExtendedSearchesByMatterResponse.SearchDetail searchDetail : extendedSearchesByMatterResponse.getSearches()) {
            if (nonNull(searchDetail.getSearch())
                && !StringUtils.isBlank(searchDetail.getSearch().getSearchId())
                && searchDetail.getSearch().getSearchId().equals(searchId)) {
                searchDetailMatch = searchDetail;
                break;
            }
        }
        if (isNull(searchDetailMatch)
            || isNull(searchDetailMatch.getSearch().getTotalCount())
            || StringUtils.isBlank(searchDetailMatch.getSearch().getName())
            || isNull(searchDetailMatch.getSearch().getIsSaved())) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("extendedSearchesByMatterResponse search data is missing for searchId: ")
                                                                 .append(searchId).append(" (total_count, name, is_saved) ").append(
                                                                     searchDetailMatch).toString(),
                                                             armRpoExecutionDetailEntity,
                                                             userAccount);
        }

        if (FALSE.equals(searchDetailMatch.getSearch().getIsSaved())) {
            log.warn(errorMessage.append("The extendedSearchesByMatterResponse is_saved attribute is FALSE for executionId: ").append(executionId).toString());
            throw new ArmRpoInProgressException("extendedSearchesByMatterResponse", executionId);
        }
        armRpoExecutionDetailEntity.setSearchItemCount(searchDetailMatch.getSearch().getTotalCount());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return searchDetailMatch.getSearch().getName();
    }

    private GetExtendedSearchesByMatterRequestGenerator createExtendedSearchesByMatterRequestGenerator(String matterId) {
        return GetExtendedSearchesByMatterRequestGenerator.builder()
            .matterId(matterId)
            .build();
    }

}
