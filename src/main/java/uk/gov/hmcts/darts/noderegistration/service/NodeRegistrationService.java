package uk.gov.hmcts.darts.noderegistration.service;


public interface NodeRegistrationService {

    Integer registerDevices(String nodeType, String courthouse,
                            String courtRoom, String hostName, String ipAddress, String macAddress);

}
