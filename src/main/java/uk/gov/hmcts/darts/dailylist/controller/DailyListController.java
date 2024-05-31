package uk.gov.hmcts.darts.dailylist.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.http.api.DailyListsApi;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListPostRequestMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.PatchDailyListRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.dailylist.validation.DailyListPostValidator;

import java.util.concurrent.CompletableFuture;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

/**
 * Default endpoints per application.
 */
@SuppressWarnings({"checkstyle.LineLengthCheck"})
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class DailyListController implements DailyListsApi {

    private final DailyListService dailyListService;
    private final DailyListProcessor processor;
    private final DailyListPostRequestMapper dailyListPostRequestMapper;

    ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
    ObjectMapper objectMapper = objectMapperConfig.objectMapper();


    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<PostDailyListResponse> dailylistsPatch(PatchDailyListRequest patchDailyListRequest) {
        DailyListJsonObject jsonDocument;
        try {
            jsonDocument = objectMapper.readValue(patchDailyListRequest.getJsonString(), DailyListJsonObject.class);
        } catch (JsonProcessingException ex) {
            throw new DartsApiException(DailyListError.FAILED_TO_PROCESS_DAILYLIST, ex);
        }

        DailyListPatchRequestInternal dailyListPatchRequest = new DailyListPatchRequestInternal();
        dailyListPatchRequest.setDailyListId(patchDailyListRequest.getDalId());
        dailyListPatchRequest.setDailyListJson(jsonDocument);
        PostDailyListResponse postDailyListResponse = dailyListService.updateDailyListInDatabase(dailyListPatchRequest);
        return new ResponseEntity<>(postDailyListResponse, HttpStatus.OK);

    }

    @Override
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/dailylists",
        produces = {"application/json", "application/json+problem"}
    )
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public ResponseEntity<PostDailyListResponse> dailylistsPost(PostDailyListRequest postDailyListRequest) {
        DailyListPostRequestInternal internalRequest = dailyListPostRequestMapper.map(postDailyListRequest);
        DailyListPostValidator.validate(internalRequest);
        PostDailyListResponse postDailyListResponse = dailyListService.saveDailyListToDatabase(internalRequest);
        return new ResponseEntity<>(postDailyListResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> dailylistsHousekeepingPost() {
        dailyListService.runHouseKeeping();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> dailylistsRunPost(String listingCourthouse) {
        if (listingCourthouse == null) {
            CompletableFuture.runAsync(processor::processAllDailyLists);
        } else {
            CompletableFuture.runAsync(() -> processor.processAllDailyListForListingCourthouse(listingCourthouse));
        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<PostDailyListResponse> dailylistsV2Patch(PatchDailyListRequest patchDailyListRequest) {
        DailyListJsonObject jsonDocument;
        try {
            jsonDocument = objectMapper.readValue(patchDailyListRequest.getJsonString(), DailyListJsonObject.class);
        } catch (JsonProcessingException ex) {
            throw new DartsApiException(DailyListError.FAILED_TO_PROCESS_DAILYLIST, ex);
        }

        DailyListPatchRequestInternal dailyListPatchRequest = new DailyListPatchRequestInternal();
        dailyListPatchRequest.setDailyListId(patchDailyListRequest.getDalId());
        dailyListPatchRequest.setDailyListJson(jsonDocument);
        PostDailyListResponse postDailyListResponse = dailyListService.updateDailyListInDatabase(dailyListPatchRequest);
        return new ResponseEntity<>(postDailyListResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<PostDailyListResponse> dailylistsV2Post(PostDailyListRequest postDailyListRequest) {
        DailyListPostRequestInternal internalRequest = dailyListPostRequestMapper.map(postDailyListRequest);
        DailyListPostValidator.validate(internalRequest);
        PostDailyListResponse postDailyListResponse = dailyListService.saveDailyListToDatabase(internalRequest);
        return new ResponseEntity<>(postDailyListResponse, HttpStatus.OK);

    }
}
