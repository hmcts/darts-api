package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

// TestClassWithoutTestCases suppression: This is not a test class.
// ConstructorCallsOverridableMethod suppression: If this proves to be a demonstrable problem, we can change the object creation approach. For now, it is fine.
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestCourtroomEntity extends CourtroomEntity implements DbInsertable<CourtroomEntity> {

    @lombok.Builder
    public TestCourtroomEntity(Integer id,
                               String name,
                               CourthouseEntity courthouse,
                               Integer createdById,
                               OffsetDateTime createdDateTime) {
        super();
        setId(id);
        setName(name);
        setCourthouse(courthouse);
        setCreatedById(createdById);
        setCreatedDateTime(createdDateTime);
    }

    @Override
    public CourtroomEntity getEntity() {
        try {
            CourtroomEntity courtroomEntity = new CourtroomEntity();
            BeanUtils.copyProperties(courtroomEntity, this);
            return courtroomEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestCourtroomEntityBuilderRetrieve implements BuilderHolder<TestCourtroomEntity, TestCourtroomEntity.TestCourtroomEntityBuilder> {

        private final TestCourtroomEntity.TestCourtroomEntityBuilder builder = TestCourtroomEntity.builder();

        @Override
        public TestCourtroomEntity build() {
            return builder.build();
        }

        @Override
        public TestCourtroomEntity.TestCourtroomEntityBuilder getBuilder() {
            return builder;
        }
    }

}