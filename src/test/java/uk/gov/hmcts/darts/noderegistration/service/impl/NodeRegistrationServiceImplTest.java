package uk.gov.hmcts.darts.noderegistration.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity_;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.noderegistration.model.GetNodeRegisterManagementResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.AvoidUsingHardCodedIP"})
class NodeRegistrationServiceImplTest {

    @Mock
    private NodeRegisterRepository nodeRegisterRepository;
    @InjectMocks
    private NodeRegistrationServiceImpl nodeRegistrationService;

    @Test
    void getNodeRegisterDevices_shouldReturnMappedResponses_whenNodeEntitiesExist() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(1);
        courthouseEntity.setDisplayName("Courthouse1");

        CourtroomEntity courtroomEntity1 = new CourtroomEntity();
        courtroomEntity1.setId(1);
        courtroomEntity1.setName("Courtroom1");
        courtroomEntity1.setCourthouse(courthouseEntity);

        CourtroomEntity courtroomEntity2 = new CourtroomEntity();
        courtroomEntity2.setId(2);
        courtroomEntity2.setName("Courtroom2");
        courtroomEntity2.setCourthouse(courthouseEntity);

        NodeRegisterEntity entity1 = new NodeRegisterEntity();
        entity1.setNodeId(1);
        entity1.setNodeType("Type1");
        entity1.setHostname("Host1");
        entity1.setIpAddress("192.168.1.1");
        entity1.setMacAddress("00:0a:95:9d:68:16");
        entity1.setCourtroom(courtroomEntity1);

        NodeRegisterEntity entity2 = new NodeRegisterEntity();
        entity2.setNodeId(2);
        entity2.setNodeType("Type2");
        entity2.setHostname("Host2");
        entity2.setIpAddress("192.168.1.2");
        entity2.setMacAddress("00:0a:95:9d:68:17");
        entity2.setCourtroom(courtroomEntity2);

        when(nodeRegisterRepository.findAll(Sort.by(NodeRegisterEntity_.NODE_ID).ascending())).thenReturn(List.of(entity1, entity2));

        List<GetNodeRegisterManagementResponse> result = nodeRegistrationService.getNodeRegisterDevices();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals("Type1", result.get(0).getNodeType());
        assertEquals("Host1", result.get(0).getHostname());
        assertEquals("192.168.1.1", result.get(0).getIpAddress());
        assertEquals("00:0a:95:9d:68:16", result.get(0).getMacAddress());
        assertEquals(1, result.get(0).getCourtroom().getId());
        assertEquals("COURTROOM1", result.get(0).getCourtroom().getName());
        assertEquals(1, result.get(0).getCourthouse().getId());
        assertEquals("Courthouse1", result.get(0).getCourthouse().getDisplayName());

        assertEquals(2, result.get(1).getId());
        assertEquals("Type2", result.get(1).getNodeType());
        assertEquals("Host2", result.get(1).getHostname());
        assertEquals("192.168.1.2", result.get(1).getIpAddress());
        assertEquals("00:0a:95:9d:68:17", result.get(1).getMacAddress());
        assertEquals(2, result.get(1).getCourtroom().getId());
        assertEquals("COURTROOM2", result.get(1).getCourtroom().getName());
        assertEquals(1, result.get(1).getCourthouse().getId());
        assertEquals("Courthouse1", result.get(1).getCourthouse().getDisplayName());
    }

    @Test
    void getNodeRegisterDevices_shouldReturnEmptyList_whenNoNodeEntitiesExist() {
        when(nodeRegisterRepository.findAll(Sort.by(NodeRegisterEntity_.NODE_ID).ascending())).thenReturn(List.of());

        List<GetNodeRegisterManagementResponse> result = nodeRegistrationService.getNodeRegisterDevices();

        assertTrue(result.isEmpty());
    }

    @Test
    void getNodeRegisterDevices_shouldHandleCourtroomAndCourthouse_whenCourtroomIsNull() {
        NodeRegisterEntity entity = new NodeRegisterEntity();
        entity.setNodeId(1);
        entity.setNodeType("Type1");
        entity.setHostname("Host1");
        entity.setIpAddress("192.168.1.1");
        entity.setMacAddress("00:0a:95:9d:68:16");
        entity.setCourtroom(null);

        when(nodeRegisterRepository.findAll(Sort.by(NodeRegisterEntity_.NODE_ID).ascending())).thenReturn(List.of(entity));

        List<GetNodeRegisterManagementResponse> result = nodeRegistrationService.getNodeRegisterDevices();

        assertEquals(1, result.size());
        assertNull(result.getFirst().getCourtroom());
        assertNull(result.getFirst().getCourthouse());
    }

    @Test
    void getNodeRegisterDevices_shouldHandleCourthouse_whenCourthouseIsNull() {
        CourtroomEntity courtroomEntity1 = new CourtroomEntity();
        courtroomEntity1.setId(1);
        courtroomEntity1.setName("Courtroom1");
        courtroomEntity1.setCourthouse(null);

        NodeRegisterEntity entity = new NodeRegisterEntity();
        entity.setNodeId(1);
        entity.setNodeType("Type1");
        entity.setHostname("Host1");
        entity.setIpAddress("192.168.1.1");
        entity.setMacAddress("00:0a:95:9d:68:16");
        entity.setCourtroom(courtroomEntity1);

        when(nodeRegisterRepository.findAll(Sort.by(NodeRegisterEntity_.NODE_ID).ascending())).thenReturn(List.of(entity));

        List<GetNodeRegisterManagementResponse> result = nodeRegistrationService.getNodeRegisterDevices();

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getCourtroom().getId());
        assertEquals("COURTROOM1", result.getFirst().getCourtroom().getName());
        assertNull(result.getFirst().getCourthouse());
    }
}