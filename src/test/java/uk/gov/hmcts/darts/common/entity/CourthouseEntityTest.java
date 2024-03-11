package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CourthouseEntityTest {

    public static final String TEST_COURTHOUSE_NAME = "Test courthouse";

    public static final int CODE = 123;

    @Test
    void adminCourtHouseEmptyRegions() {

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        Set<RegionEntity> regions = new LinkedHashSet<>();

        courthouseEntity.setRegions(regions);

        assertNull(courthouseEntity.getRegion());
    }

    @Test
    void adminCourtHouseNullRegions() {

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setRegions(null);
        courthouseEntity.setRegion(null);

        assertNull(courthouseEntity.getRegion());
    }

    @Test
    void adminCourtHouseSetOneRegionAndNoRegions() {

        CourthouseEntity courthouseEntity = new CourthouseEntity();

        RegionEntity region1 = new RegionEntity();
        region1.setId(5);

        courthouseEntity.setRegions(null);
        courthouseEntity.setRegion(region1);

        assertEquals(5, courthouseEntity.getRegion().getId());
    }

    @Test
    void adminCourtHouseSetRegionsAndRegion() {

        Set<RegionEntity> regions = new LinkedHashSet<>();
        RegionEntity region1 = new RegionEntity();
        region1.setId(5);
        RegionEntity region2 = new RegionEntity();
        region2.setId(6);
        regions.add(region1);

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setRegions(regions);
        courthouseEntity.setRegion(region2);

        assertEquals(6, courthouseEntity.getRegion().getId());
    }

    @Test
    void adminCourtHouseSetRegionsWithTwoRegionEntities() {

        Set<RegionEntity> regions = new LinkedHashSet<>();
        RegionEntity region1 = new RegionEntity();
        region1.setId(5);
        RegionEntity region2 = new RegionEntity();
        region2.setId(6);
        regions.add(region1);
        regions.add(region2);

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setRegions(regions);

        Exception exception = assertThrows(IllegalStateException.class, courthouseEntity::getRegion);

        assertNull(exception.getMessage());

    }

    @Test
    void adminCourtHouseSetOneRegion() {

        CourthouseEntity courthouseEntity = new CourthouseEntity();

        Set<RegionEntity> regions = new LinkedHashSet<>();
        RegionEntity region1 = new RegionEntity();
        region1.setId(5);

        courthouseEntity.setRegions(regions);
        courthouseEntity.setRegion(region1);

        assertEquals(5, courthouseEntity.getRegion().getId());

    }

    @Test
    void adminCourtHouseSecurityRegions() {

        Set<SecurityGroupEntity> securityGroups = new LinkedHashSet<>();
        SecurityGroupEntity securityGroup1 = new SecurityGroupEntity();
        securityGroup1.setId(3);
        SecurityGroupEntity securityGroup2 = new SecurityGroupEntity();
        securityGroup2.setId(4);

        securityGroups.add(securityGroup1);
        securityGroups.add(securityGroup2);

        CourthouseEntity courthouseEntity = new CourthouseEntity();

        courthouseEntity.setSecurityGroups(securityGroups);

        Set<SecurityGroupEntity> secGrps = courthouseEntity.getSecurityGroups();
        Set<Integer> expectedList = new LinkedHashSet<>(Arrays.asList(3, 4));
        Set<Integer> actualList = secGrps.stream().map(SecurityGroupEntity::getId).collect(Collectors.toSet());

        assertEquals(expectedList, actualList);

    }

}