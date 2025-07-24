package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestNodeRegisterEntity extends NodeRegisterEntity implements DbInsertable<NodeRegisterEntity> {

    @lombok.Builder
    public TestNodeRegisterEntity(
        Integer id,
        CourtroomEntity courtroom,
        String hostname,
        String ipAddress,
        String macAddress,
        String nodeType,
        OffsetDateTime createdDateTime,
        Integer createdById
    ) {
        super();
        setNodeId(id);
        setCourtroom(courtroom);
        setHostname(hostname);
        setIpAddress(ipAddress);
        setMacAddress(macAddress);
        setNodeType(nodeType);
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
    }

    @Override
    public NodeRegisterEntity getEntity() {
        try {
            NodeRegisterEntity nodeRegisterEntity = new NodeRegisterEntity();
            BeanUtils.copyProperties(nodeRegisterEntity, this);
            return nodeRegisterEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestNodeRegisterEntityBuilderRetrieve
        implements BuilderHolder<TestNodeRegisterEntity, TestNodeRegisterEntity.TestNodeRegisterEntityBuilder> {
       private final TestNodeRegisterEntity.TestNodeRegisterEntityBuilder builder = TestNodeRegisterEntity.builder();

        @Override
        public TestNodeRegisterEntity build() {
            return builder.build();
        }

        @Override
        public TestNodeRegisterEntity.TestNodeRegisterEntityBuilder getBuilder() {
            return builder;
        }
    }
}