package uk.gov.hmcts.darts.noderegistration.service;


import uk.gov.hmcts.darts.noderegistration.model.GetNodeRegisterManagementResponse;

import java.util.List;

public interface NodeRegistrationService {

    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    Integer registerDevices(String nodeType, String courthouse,
        String courtRoom, String hostName, String ipAddress, String macAddress);

    List<GetNodeRegisterManagementResponse> getNodeRegisterDevices();

}
