package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
                                UserAccountEntity createdBy,
                                OffsetDateTime createdDateTime,
                                UserAccountEntity lastModifiedBy,
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
        setCreatedBy(createdBy);
        setCreatedDateTime(createdDateTime);
        setLastModifiedBy(lastModifiedBy);
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

