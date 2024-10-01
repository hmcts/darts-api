package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.repository.RegionRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Deprecated
public class RegionStub {
    private final RegionRepository regionRepository;

    public RegionEntity createRegionsUnlessExists(String name) {
        Optional<RegionEntity> foundRegion = regionRepository.findByRegionNameIgnoreCase(name);
        return foundRegion.orElseGet(() -> createRegion(name));
    }

    private RegionEntity createRegion(String name) {
        RegionEntity newRegion = new RegionEntity();
        newRegion.setRegionName(name);
        regionRepository.saveAndFlush(newRegion);
        return newRegion;
    }
}