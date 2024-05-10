package uk.gov.hmcts.darts.noderegistration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.noderegistration.service.NodeRegistrationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeRegistrationServiceImpl implements NodeRegistrationService {

    private final NodeRegisterRepository nodeRegisterRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserIdentity userIdentity;

    @Override
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    public Integer registerDevices(String nodeType, String courthouse, String courtRoom, String hostName, String ipAddress, String macAddress) {
        CourtroomEntity courtroomEntity = retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtRoom);
        NodeRegisterEntity nodeRegisterEntity = new NodeRegisterEntity();
        nodeRegisterEntity.setCourtroom(courtroomEntity);
        nodeRegisterEntity.setNodeType(nodeType);
        nodeRegisterEntity.setHostname(hostName);
        nodeRegisterEntity.setIpAddress(ipAddress);
        nodeRegisterEntity.setMacAddress(macAddress);
        nodeRegisterEntity.setCreatedBy(userIdentity.getUserAccount());
        return nodeRegisterRepository.saveAndFlush(nodeRegisterEntity).getNodeId();
    }
}