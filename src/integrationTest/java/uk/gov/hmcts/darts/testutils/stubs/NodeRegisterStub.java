package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;

@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidUsingHardCodedIP"})
public class NodeRegisterStub {
    private final NodeRegisterRepository nodeRegisterRepository;

    public void setupNodeRegistry(CourtroomEntity courtroom) {
        NodeRegisterEntity nodeRegisterEntity = new NodeRegisterEntity();
        nodeRegisterEntity.setNodeType("DAR");
        nodeRegisterEntity.setCourtroom(courtroom);
        nodeRegisterEntity.setIpAddress("1.2.3.4");
        nodeRegisterEntity.setHostname("theHostname");
        nodeRegisterEntity.setMacAddress("theMacAddress");
        nodeRegisterRepository.save(nodeRegisterEntity);
    }

}
