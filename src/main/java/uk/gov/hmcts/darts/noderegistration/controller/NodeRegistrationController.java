package uk.gov.hmcts.darts.noderegistration.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.noderegistration.http.api.DevicesApi;
import uk.gov.hmcts.darts.noderegistration.model.GetNodeRegisterManagementResponse;
import uk.gov.hmcts.darts.noderegistration.model.PostNodeRegistrationResponse;
import uk.gov.hmcts.darts.noderegistration.service.NodeRegistrationService;

import java.util.List;
import javax.validation.Valid;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class NodeRegistrationController implements DevicesApi {

    private final NodeRegistrationService nodeRegistrationService;

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {MID_TIER, DAR_PC})
    @SuppressWarnings({"PMD.UnnecessaryAnnotationValueElement", "PMD.UseObjectForClearerAPI"})
    @Override
    public ResponseEntity<PostNodeRegistrationResponse> registerDevicesPost(
        @Parameter(name = "node_type", description = "The type of device being registered, might just be DAR", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "node_type") String nodeType,
        @Parameter(name = "courthouse", description = "The name of the location of the courtroom containing the device", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "courthouse") String courtHouse,
        @Parameter(name = "courtroom", description = "The name of the courtroom containing the device", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "courtroom") String courtRoom,
        @Parameter(name = "host_name", description = "The host name of the device on the network", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "host_name") String hostName,
        @Parameter(name = "ip_address", description = "The IP address of the device on the network", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "ip_address") String ipAddress,
        @Parameter(name = "mac_address", description = "Tha MAC address of the device", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "mac_address") String macAddress
    ) {
        Integer nodeId = nodeRegistrationService.registerDevices(nodeType, courtHouse, courtRoom,
                                                                 hostName, ipAddress, macAddress
        );

        PostNodeRegistrationResponse postRegisterDeviceResponse = new PostNodeRegistrationResponse();
        postRegisterDeviceResponse.setNodeId(nodeId);
        return new ResponseEntity<>(postRegisterDeviceResponse, HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    @Override
    public ResponseEntity<List<GetNodeRegisterManagementResponse>> adminNodeRegisterManagementGet() {
        return new ResponseEntity<>(nodeRegistrationService.getNodeRegisterDevices(), HttpStatus.OK);
    }
}
