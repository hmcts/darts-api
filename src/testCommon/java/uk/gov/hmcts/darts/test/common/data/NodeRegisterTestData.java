package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestNodeRegisterEntity;

@SuppressWarnings({"PMD.AvoidUsingHardCodedIP"})
public final class NodeRegisterTestData
    implements Persistable<TestNodeRegisterEntity.TestNodeRegisterEntityBuilderRetrieve, NodeRegisterEntity,
    TestNodeRegisterEntity.TestNodeRegisterEntityBuilder> {

    NodeRegisterTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    @Override
    public NodeRegisterEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }


    @Override
    public TestNodeRegisterEntity.TestNodeRegisterEntityBuilderRetrieve someMinimalBuilderHolder() {
        var builderRetrieve = new TestNodeRegisterEntity.TestNodeRegisterEntityBuilderRetrieve();

        builderRetrieve.getBuilder()
            .courtroom(PersistableFactory.getCourtroomTestData().someMinimal())
            .hostname("Host1")
            .ipAddress("192.168.1.3")
            .macAddress("00:0a:95:9d:68:21")
            .nodeType("DAR")
            .createdById(0);

        return builderRetrieve;
    }

    @Override
    public TestNodeRegisterEntity.TestNodeRegisterEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}