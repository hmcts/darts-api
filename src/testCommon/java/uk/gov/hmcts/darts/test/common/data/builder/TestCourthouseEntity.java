package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// TestClassWithoutTestCases suppression: This is not a test class.
// ConstructorCallsOverridableMethod suppression: If this proves to be a demonstrable problem, we can change the object creation approach. For now, it is fine.
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestCourthouseEntity extends CourthouseEntity implements DbInsertable<CourthouseEntity> {

    @lombok.Builder
    public TestCourthouseEntity(Integer id,
                                Integer code,
                                String courthouseName,
                                List<CourtroomEntity> courtrooms,
                                Set<SecurityGroupEntity> securityGroups,
                                Set<RegionEntity> regions,
                                String displayName,
                                String courthouseObjectId,
                                String folderPath,
                                Integer createdById,
                                OffsetDateTime createdDateTime,
                                Integer lastModifiedById,
                                OffsetDateTime lastModifiedDateTime) {
        setId(id);
        setCode(code);
        setCourthouseName(courthouseName);
        setCourtrooms(courtrooms != null ? courtrooms : new ArrayList<>());
        setSecurityGroups(securityGroups != null ? securityGroups : new LinkedHashSet<>());
        setRegions(regions != null ? regions : new LinkedHashSet<>());
        setDisplayName(displayName);
        setCourthouseObjectId(courthouseObjectId);
        setFolderPath(folderPath);
        setCreatedById(createdById);
        setCreatedDateTime(createdDateTime);
        setLastModifiedById(lastModifiedById);
        setLastModifiedDateTime(lastModifiedDateTime);
    }

    @Override
    public CourthouseEntity getEntity() {
        try {
            CourthouseEntity courthouseEntity = new CourthouseEntity();
            BeanUtils.copyProperties(courthouseEntity, this);
            return courthouseEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestCourthouseEntityBuilderRetrieve implements BuilderHolder<TestCourthouseEntity, TestCourthouseEntity.TestCourthouseEntityBuilder> {

        private final TestCourthouseEntity.TestCourthouseEntityBuilder builder = TestCourthouseEntity.builder();

        @Override
        public TestCourthouseEntity build() {
            return builder.build();
        }

        @Override
        public TestCourthouseEntity.TestCourthouseEntityBuilder getBuilder() {
            return builder;
        }
    }

}

