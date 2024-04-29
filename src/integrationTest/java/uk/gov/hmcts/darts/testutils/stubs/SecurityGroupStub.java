package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class SecurityGroupStub {
    private final SecurityGroupRepository securityGroupRepository;


    @Transactional
    public void addCourthouse(SecurityGroupEntity securityGroupEntity, CourthouseEntity courthouseEntity) {
        Optional<SecurityGroupEntity> securityGroup = securityGroupRepository.findById(securityGroupEntity.getId());
        securityGroup.get().getCourthouseEntities().add(courthouseEntity);
    }
}
