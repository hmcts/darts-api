package uk.gov.hmcts.darts.noderegistration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.noderegistration.model.GetNodeRegisterManagementResponse;
import uk.gov.hmcts.darts.noderegistration.model.GetNodeRegisterManagementResponseCourthouse;
import uk.gov.hmcts.darts.noderegistration.model.GetNodeRegisterManagementResponseCourtroom;
import uk.gov.hmcts.darts.noderegistration.service.NodeRegistrationService;

import java.util.List;

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
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        CourtroomEntity courtroomEntity = retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtRoom, userAccount);
        NodeRegisterEntity nodeRegisterEntity = new NodeRegisterEntity();
        nodeRegisterEntity.setCourtroom(courtroomEntity);
        nodeRegisterEntity.setNodeType(nodeType);
        nodeRegisterEntity.setHostname(hostName);
        nodeRegisterEntity.setIpAddress(ipAddress);
        nodeRegisterEntity.setMacAddress(macAddress);
        nodeRegisterEntity.setCreatedBy(userAccount);
        return nodeRegisterRepository.saveAndFlush(nodeRegisterEntity).getNodeId();
    }

    @Override
    public List<GetNodeRegisterManagementResponse> getNodeRegisterDevices() {
        return nodeRegisterRepository.findAll().stream()
            .map(nodeRegisterEntity -> new GetNodeRegisterManagementResponse()
                .id(nodeRegisterEntity.getNodeId())
                .nodeType(nodeRegisterEntity.getNodeType())
                .courthouse(mapToCourthouse(nodeRegisterEntity.getCourtroom()))
                .courtroom(mapToCourtroom(nodeRegisterEntity.getCourtroom()))
                .hostname(nodeRegisterEntity.getHostname())
                .ipAddress(nodeRegisterEntity.getIpAddress())
                .macAddress(nodeRegisterEntity.getMacAddress())
                .createdAt(nodeRegisterEntity.getCreatedDateTime())
                .createdBy(nodeRegisterEntity.getCreatedById()))
            .toList();
    }

    public static GetNodeRegisterManagementResponseCourthouse mapToCourthouse(CourtroomEntity courtroomEntity) {
        if (courtroomEntity == null || courtroomEntity.getCourthouse() == null) {
            return null;
        }
        GetNodeRegisterManagementResponseCourthouse courthouse = new GetNodeRegisterManagementResponseCourthouse();
        courthouse.setId(courtroomEntity.getCourthouse().getId());
        courthouse.setDisplayName(courtroomEntity.getCourthouse().getDisplayName());
        return courthouse;
    }

    public static GetNodeRegisterManagementResponseCourtroom mapToCourtroom(CourtroomEntity courtroomEntity) {
        if (courtroomEntity == null) {
            return null;
        }
        GetNodeRegisterManagementResponseCourtroom courtroom = new GetNodeRegisterManagementResponseCourtroom();
        courtroom.setId(courtroomEntity.getId());
        courtroom.setName(courtroomEntity.getName());
        return courtroom;
    }
}