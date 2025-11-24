package uk.gov.hmcts.darts.arm.rpo.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.SaveBackgroundSearchService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
public class SaveBackgroundSearchServiceImpl implements SaveBackgroundSearchService {

    private static final String AND_RESPONSE = " and response - ";
    public static final String SEARCH_WITH_NO_RESULTS = "Search with no results";

    private final ArmClientService armClientService;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        log.info("saveBackgroundSearch called with executionId: {}, searchName: {}", executionId, searchName);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.saveBackgroundSearchRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        StringBuilder errorMessage = new StringBuilder(134).append("Failure during ARM save background search: ");
        SaveBackgroundSearchResponse saveBackgroundSearchResponse = null;
        try {
            SaveBackgroundSearchRequest saveBackgroundSearchRequest =
                createSaveBackgroundSearchRequest(searchName, armRpoExecutionDetailEntity.getSearchId());
            saveBackgroundSearchResponse = armClientService.saveBackgroundSearch(bearerToken, saveBackgroundSearchRequest);
        } catch (FeignException feignException) {
            log.error(errorMessage.append("Unable to save background search").append(feignException).toString());
            processSaveBackgroundSearchException(userAccount, feignException, errorMessage, armRpoExecutionDetailEntity);
        }
        log.info("ARM RPO Response - SaveBackgroundSearchResponse: {}", saveBackgroundSearchResponse);
        armRpoUtil.handleResponseStatus(userAccount, saveBackgroundSearchResponse, errorMessage, armRpoExecutionDetailEntity);

        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    private BaseRpoResponse getBaseRpoResponse(UserAccountEntity userAccount, FeignException feignException, StringBuilder errorMessage,
                                               ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        String feignResponse = feignException.contentUTF8();
        if (StringUtils.isEmpty(feignResponse)) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(ArmRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException).toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }
        log.debug("ARM RPO Response - Feign response: {}", feignResponse);
        BaseRpoResponse baseRpoResponse;
        try {
            baseRpoResponse = objectMapper.readValue(feignResponse, BaseRpoResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to parse feign response: {}", feignResponse, ex);
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append(ArmRpoUtil.UNABLE_TO_GET_ARM_RPO_RESPONSE).append(feignException).toString(),
                                                             armRpoExecutionDetailEntity, userAccount, ex);
        }
        return baseRpoResponse;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    @SneakyThrows
    private void processSaveBackgroundSearchException(UserAccountEntity userAccount,
                                                      FeignException feignException, StringBuilder errorMessage,
                                                      ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        BaseRpoResponse baseRpoResponse = getBaseRpoResponse(userAccount, feignException, errorMessage, armRpoExecutionDetailEntity);
        if (isNull(baseRpoResponse) || isNull(baseRpoResponse.getStatus()) || isNull(baseRpoResponse.getIsError())) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API saveBackgroundSearch is invalid - ")
                                                                 .append(baseRpoResponse).toString(),
                                                             armRpoExecutionDetailEntity, userAccount);
        }
        try {
            HttpStatus httpStatus = HttpStatus.valueOf(baseRpoResponse.getStatus());

            if (HttpStatus.BAD_REQUEST.value() == httpStatus.value() && baseRpoResponse.getMessage().contains(SEARCH_WITH_NO_RESULTS)) {
                log.warn("Background search has no results, marking RPO as failed");
                armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
                throw new ArmRpoException(errorMessage.toString(), null);
            }
        } catch (IllegalArgumentException e) {
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API baseRpoResponse status is invalid - ")
                                                                 .append(baseRpoResponse).toString(),
                                                             armRpoExecutionDetailEntity, userAccount, e);
        }
        throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("ARM RPO API saveBackgroundSearch is not valid - ")
                                                             .append(baseRpoResponse).toString(),
                                                         armRpoExecutionDetailEntity, userAccount);
    }

    private SaveBackgroundSearchRequest createSaveBackgroundSearchRequest(String searchName, String searchId) {
        return SaveBackgroundSearchRequest.builder()
            .name(searchName)
            .searchId(searchId)
            .build();
    }

}
