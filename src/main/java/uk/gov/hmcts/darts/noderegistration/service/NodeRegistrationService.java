package uk.gov.hmcts.darts.noderegistration.service;


public interface NodeRegistrationService {

    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    Integer registerDevices(String nodeType, String courthouse,
        String courtRoom, String hostName, String ipAddress, String macAddress);

}
