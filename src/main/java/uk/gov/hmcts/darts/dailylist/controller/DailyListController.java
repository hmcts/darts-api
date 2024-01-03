package uk.gov.hmcts.darts.dailylist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.http.api.DailyListsApi;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;
import uk.gov.hmcts.darts.dailylist.model.Problem;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.dailylist.validation.DailyListPostValidator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
public class DailyListController implements DailyListsApi {

    private final CourthouseRepository courthouseRepository;
    private final DailyListService dailyListService;
    private final DailyListProcessor processor;

    ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
    ObjectMapper objectMapper = objectMapperConfig.objectMapper();


    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<PostDailyListResponse> dailylistsPatch(
        @NotNull @Parameter(name = "dal_id", description = "ID of the DailyList in the database.", required = true, in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "dal_id", required = true) Integer dalId,
        @jakarta.validation.constraints.NotNull @Parameter(name = "json_string", description = "JSON representation of the 'document' received in the " +
            "addDocument request.<p> **Conditional mandatory** either this or xml_document needs to be provided, or both.",
            required = true, in = ParameterIn.HEADER)
        @RequestHeader(value = "json_string", required = true) String jsonString
    ) {
        DailyListJsonObject jsonDocument = objectMapper.readValue(jsonString, DailyListJsonObject.class);

        DailyListPatchRequest dailyListPatchRequest = new DailyListPatchRequest();
        dailyListPatchRequest.setDailyListId(dalId);
        dailyListPatchRequest.setDailyListJson(jsonDocument);
        PostDailyListResponse postDailyListResponse = dailyListService.updateDailyListInDatabase(dailyListPatchRequest);
        return new ResponseEntity<>(postDailyListResponse, HttpStatus.OK);

    }

    @SneakyThrows
    @Override
    @Operation(
        operationId = "dailylistsPost",
        summary = "XHIBIT/CPP send daily case lists to the DAR PC via DARTS. These daily case lists inform the DAR PC which cases are being heard that day " +
            "within the courthouse for all of its courtrooms.",
        description = "description",
        tags = {"DailyLists"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Created", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = PostDailyListResponse.class)),
                @Content(mediaType = "application/json+problem", schema = @Schema(implementation = PostDailyListResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class)),
                @Content(mediaType = "application/json+problem", schema = @Schema(implementation = Problem.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/dailylists",
        produces = {"application/json", "application/json+problem"}
    )
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<PostDailyListResponse> dailylistsPost(
        @NotNull @Parameter(name = "source_system", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "source_system", required = true) String sourceSystem,
        @Parameter(name = "courthouse", description = "The courthouse that the dailyList represents. <p> **" +
            "Conditional mandatory**, required if json_document not provided", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "courthouse", required = false) String courthouse,
        @Parameter(name = "hearing_date", description = "The date that the dailyList represents. <p> **Conditional mandatory**, required if " +
            "json_document not provided", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "hearing_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hearingDate,
        @Parameter(name = "unique_id", description = "The uniqueId. <p> **Conditional mandatory**, required if " +
            "json_document not provided", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "unique_id", required = false) String uniqueId,
        @Parameter(name = "published_ts", description = "The date that the dailyList was published. <p> **Conditional mandatory**, required if " +
            "json_document not provided", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "published_ts", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime publishedTs,
        @Parameter(name = "xml_document", description = "XML representation of the 'document' received in the addDocument request.<p> " +
            "**Conditional mandatory** either this or json_document needs to be provided, or both. This will not be parsed but just " +
            "stored in the database as a string", in = ParameterIn.HEADER) @RequestHeader(value = "xml_document", required = false) String xmlDocument,
        @Parameter(name = "json_string", description = "JSON representation of the 'document' received in the addDocument request.<p>"
            + "**Conditional mandatory** either this or xml_document needs to be provided, or both.",
            in = ParameterIn.HEADER) @RequestHeader(value = "json_string", required = false) String jsonString
    ) {
        DailyListJsonObject jsonDocument = objectMapper.readValue(jsonString, DailyListJsonObject.class);

        DailyListPostRequest postRequest = new DailyListPostRequest();
        postRequest.setSourceSystem(sourceSystem);
        postRequest.setCourthouse(courthouse);
        postRequest.setDailyListXml(xmlDocument);
        postRequest.setDailyListJson(jsonDocument);
        postRequest.setHearingDate(hearingDate);
        postRequest.setUniqueId(uniqueId);
        postRequest.setPublishedDateTime(publishedTs);

        DailyListPostValidator.validate(postRequest);
        PostDailyListResponse postDailyListResponse = dailyListService.saveDailyListToDatabase(postRequest);
        return new ResponseEntity<>(postDailyListResponse, HttpStatus.OK);

    }

    public ResponseEntity<Void> dailylistsHousekeepingPost() {
        dailyListService.runHouseKeepingNow();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> dailylistsRunPost(Integer courthouseId) {

        if (courthouseId == null) {
            CompletableFuture.runAsync(() -> processor.processAllDailyLists(LocalDate.now()));
        } else {
            Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findById(
                courthouseId);
            foundCourthouse.ifPresentOrElse(
                courthouse -> CompletableFuture.runAsync(() -> processor.processAllDailyListForCourthouse(courthouse)),
                () -> {
                    throw new DartsApiException(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST);
                }
            );
        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
