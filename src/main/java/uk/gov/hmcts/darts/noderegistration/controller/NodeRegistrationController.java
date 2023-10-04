package uk.gov.hmcts.darts.noderegistration.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.noderegistration.api.DevicesApi;
import uk.gov.hmcts.darts.noderegistration.model.PostNodeRegistrationResponse;
import uk.gov.hmcts.darts.noderegistration.service.NodeRegistrationService;

import javax.validation.Valid;

@RestController
public class NodeRegistrationController implements DevicesApi {

    @Autowired
    NodeRegistrationService nodeRegistrationService;

    public ResponseEntity<PostNodeRegistrationResponse> registerDevicesPost(
        @Parameter(name = "node_type", description = "The type of device being registered, might just be DAR", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "node_type") String nodeType,
        @Parameter(name = "courthouse", description = "The name of the location of the courtroom containing the device", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "courthouse") String courtHouse,
        @Parameter(name = "court_room", description = "The name of the courtroom containing the device", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "court_room") String courtRoom,
        @Parameter(name = "host_name", description = "The host name of the device on the network", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "host_name") String hostName,
        @Parameter(name = "ip_address", description = "The IP address of the device on the network", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "ip_address") String ipAddress,
        @Parameter(name = "mac_address", description = "Tha MAC address of the device", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "mac_address") String macAddress
    ) {
        Integer nodeId = nodeRegistrationService.registerDevices(nodeType, courtHouse, courtRoom,
              hostName, ipAddress, macAddress);

        PostNodeRegistrationResponse postRegisterDeviceResponse = new PostNodeRegistrationResponse();
        postRegisterDeviceResponse.setNodeId(nodeId);
        return new ResponseEntity<>(postRegisterDeviceResponse, HttpStatus.OK);
    }
}
