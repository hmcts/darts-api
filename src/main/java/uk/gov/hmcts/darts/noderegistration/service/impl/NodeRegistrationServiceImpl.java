package uk.gov.hmcts.darts.noderegistration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.noderegistration.exception.NodeRegistrationApiError;
import uk.gov.hmcts.darts.noderegistration.repository.NodeRegistrationRepository;
import uk.gov.hmcts.darts.noderegistration.service.NodeRegistrationService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeRegistrationServiceImpl implements NodeRegistrationService {

    private final NodeRegistrationRepository nodeRegistrationRepository;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;

    @Override
    public Integer registerDevices(String nodeType, String courthouse, String courtRoom, String hostName, String ipAddress, String macAddress) {
        Optional<CourtroomEntity> courtroomEntity = courtroomRepository.findByCourthouseNameAndCourtroomName(courthouse, courtRoom);
        if (courtroomEntity.isPresent()) {
            NodeRegisterEntity nodeRegisterEntity = new NodeRegisterEntity();
            nodeRegisterEntity.setCourtroom(courtroomEntity.get());
            nodeRegisterEntity.setDeviceType(nodeType);
            nodeRegisterEntity.setHostname(hostName);
            nodeRegisterEntity.setIpAddress(ipAddress);
            nodeRegisterEntity.setMacAddress(macAddress);

            return nodeRegistrationRepository.saveAndFlush(nodeRegisterEntity).getNodeId();
        }
        throw new DartsApiException(NodeRegistrationApiError.INVALID_COURTROOM);
    }
}
