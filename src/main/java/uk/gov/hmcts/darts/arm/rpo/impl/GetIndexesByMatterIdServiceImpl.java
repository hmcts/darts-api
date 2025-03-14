package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetIndexesByMatterIdService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
public class GetIndexesByMatterIdServiceImpl implements GetIndexesByMatterIdService {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

    @Override
    public void getIndexesByMatterId(String bearerToken, Integer executionId, String matterId, UserAccountEntity userAccount) {
        log.debug("getIndexesByMatterId called with executionId: {}, matterId: {}", executionId, matterId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getIndexesByMatterIdRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(151).append("Failure during ARM RPO get indexes by matter ID: ");
        IndexesByMatterIdResponse indexesByMatterIdResponse;
        try {
            indexesByMatterIdResponse = armRpoClient.getIndexesByMatterId(bearerToken, createIndexesByMatterIdRequest(matterId));
        } catch (FeignException e) {
            log.error(errorMessage.append(ArmRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(e).toString(), e);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - IndexesByMatterIdResponse: {}", indexesByMatterIdResponse);
        processIndexesByMatterIdResponse(matterId, userAccount, indexesByMatterIdResponse, errorMessage, armRpoExecutionDetailEntity);
    }

    private void processIndexesByMatterIdResponse(String matterId, UserAccountEntity userAccount, IndexesByMatterIdResponse indexesByMatterIdResponse,
                                                  StringBuilder errorMessage,
                                                  ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        armRpoUtil.handleResponseStatus(userAccount, indexesByMatterIdResponse, errorMessage, armRpoExecutionDetailEntity);

        List<IndexesByMatterIdResponse.Index> indexes = indexesByMatterIdResponse.getIndexes();
        if (CollectionUtils.isEmpty(indexes)
            || isNull(indexes.getFirst())
            || isNull(indexes.getFirst().getIndex())
            || StringUtils.isBlank(indexes.getFirst().getIndex().getIndexId())) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("Unable to find any indexes by matter ID in response").toString(),
                                                             armRpoExecutionDetailEntity,
                                                             userAccount);
        }

        String indexId = indexes.getFirst().getIndex().getIndexId();
        if (indexes.size() > 1) {
            log.warn("More than one index found in response for matterId: {}. Using first index id: {} from response: {}",
                     matterId, indexId, indexesByMatterIdResponse);
        }
        armRpoExecutionDetailEntity.setIndexId(indexId);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    private IndexesByMatterIdRequest createIndexesByMatterIdRequest(String matterId) {
        return IndexesByMatterIdRequest.builder()
            .matterId(matterId)
            .build();
    }
}
