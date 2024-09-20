package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Deprecated
@SuppressWarnings({"PMD.AvoidUsingHardCodedIP"})
public class NodeRegisterStub {
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;
    private final UserAccountRepository userAccountRepository;

    public void setupNodeRegistry(CourtroomEntity courtroom) {
        NodeRegisterEntity nodeRegisterEntity = new NodeRegisterEntity();
        nodeRegisterEntity.setNodeType("DAR");
        nodeRegisterEntity.setCourtroom(courtroom);
        nodeRegisterEntity.setIpAddress("1.2.3.4");
        nodeRegisterEntity.setHostname("theHostname");
        nodeRegisterEntity.setMacAddress("theMacAddress");
        nodeRegisterEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        nodeRegisterEntity.setCreatedDateTime(OffsetDateTime.now());
        dartsDatabaseSaveStub.save(nodeRegisterEntity);
    }

}