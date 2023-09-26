package uk.gov.hmcts.darts.noderegistration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;
import uk.gov.hmcts.darts.dailylist.model.Problem;
import uk.gov.hmcts.darts.dailylist.validation.DailyListPostValidator;
import uk.gov.hmcts.darts.noderegistration.api.DevicesApi;
import uk.gov.hmcts.darts.noderegistration.model.PostNodeRegistrationResponse;
import uk.gov.hmcts.darts.noderegistration.model.PostRegisterDeviceResponse;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
public class NodeRegistrationController implements DevicesApi {


    public ResponseEntity<PostNodeRegistrationResponse> registerDevicesPost(
        @Parameter(name = "node_type", description = "???", in = ParameterIn.QUERY) @Valid @RequestParam(value = "node_type", required = false) String nodeType,
        @Parameter(name = "courthouse", description = "???", in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = false) String courtHouse,
        @Parameter(name = "court_room", description = "???", in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_room", required = false) String courtRoom,
        @Parameter(name = "host_name", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "host_name", required = false) String hostName,
        @Parameter(name = "ip_address", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "ip_address", required = false) String ipAddress,
        @Parameter(name = "mac_address", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "mac_address", required = false) String macAddress
    ) {
        PostNodeRegistrationResponse postRegisterDeviceResponse = new PostNodeRegistrationResponse();
        postRegisterDeviceResponse.setNodeId(101);
        return new ResponseEntity<>(postRegisterDeviceResponse, HttpStatus.OK);
    }

    /*
    @Override
    @Operation(
        operationId = "registerDevicesPost",
        summary = "?????",
        description = "description",
        tags = {"Devices"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Created", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = PostRegisterDeviceResponse.class)),
                @Content(mediaType = "application/json+problem", schema = @Schema(implementation = PostRegisterDeviceResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class)),
                @Content(mediaType = "application/json+problem", schema = @Schema(implementation = Problem.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/register-devices",
        produces = {"application/json", "application/json+problem"}
    )
    public ResponseEntity<PostDailyListResponse> registerDevicesPost(
        //@NotNull @Parameter(name = "source_system", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "source_system", required = true) String sourceSystem,
        @Parameter(name = "node_type", description = "???", in = ParameterIn.QUERY) @Valid @RequestParam(value = "node_type", required = false) String nodeType,
        @Parameter(name = "court_house", description = "???", in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_house", required = false) String courtHouse,
        @Parameter(name = "court_room", description = "???", in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_room", required = false) String courtRoom,
        @Parameter(name = "host_name", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "host_name", required = false) String hostName,
        @Parameter(name = "ip_address", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "ip_address", required = false) String ipAddress,
        @Parameter(name = "mac_address", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "mac_address", required = false) String macAddress

    ) {

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


        return new ResponseEntity<>(null, HttpStatus.OK);
    }

     */
}
