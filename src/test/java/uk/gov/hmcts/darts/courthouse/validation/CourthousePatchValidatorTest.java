package uk.gov.hmcts.darts.courthouse.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NOT_FOUND;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.SECURITY_GROUP_ID_DOES_NOT_EXIST;

@ExtendWith(MockitoExtension.class)
class CourthousePatchValidatorTest {

    @Mock
    private CourthouseRepository courthouseRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private SecurityGroupRepository securityGroupRepository;

    private CourthousePatchValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CourthousePatchValidator(
            courthouseRepository,
            caseRepository,
            regionRepository,
            securityGroupRepository);
    }

    @Test
    void throwsIfCourthouseDoesntExist() {
        when(courthouseRepository.findById(1)).thenReturn(Optional.empty());

        CourthousePatch courthousePatch = new CourthousePatch();
        assertThatThrownBy(() -> validator.validate(courthousePatch, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", COURTHOUSE_NOT_FOUND);
    }

    @Test
    void throwsWhenCourthouseNamePatchProvidedForCourthouseWithCases() {
        var targetCourthouse = someCourthouse();
        when(courthouseRepository.findById(1)).thenReturn(Optional.of(targetCourthouse));
        when(caseRepository.existsByCourthouse(targetCourthouse)).thenReturn(true);

        CourthousePatch courthousePatch = someCourthousePatchWithCourthouseName("some-new-name");
        assertThatThrownBy(() -> validator.validate(courthousePatch, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING);
    }

    @Test
    void throwsWhenCourthouseNameAlreadyExists() {
        when(courthouseRepository.findById(1)).thenReturn(Optional.of(someCourthouse()));
        when(caseRepository.existsByCourthouse(any())).thenReturn(false);
        when(courthouseRepository.existsByCourthouseNameAndIdNot("SOME-ALREADY-EXISTING-NAME", 1)).thenReturn(true);

        CourthousePatch courthousePatch = someCourthousePatchWithCourthouseName("some-already-existing-name");
        assertThatThrownBy(() -> validator.validate(courthousePatch, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
    }

    @Test
    void throwsWhenCourthouseDisplayNameAlreadyExists() {
        when(courthouseRepository.findById(1)).thenReturn(Optional.of(someCourthouse()));
        when(courthouseRepository.existsByDisplayNameIgnoreCaseAndIdNot("some-already-existing-display-name", 1)).thenReturn(true);

        CourthousePatch courthousePatch = someCourthousePatchWithCourthouseDisplayName("some-already-existing-display-name");
        assertThatThrownBy(() -> validator.validate(courthousePatch, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS);
    }

    @Test
    void throwsWhenRegionDoesntExist() {
        when(courthouseRepository.findById(1)).thenReturn(Optional.of(someCourthouse()));
        when(regionRepository.existsById(2)).thenReturn(false);

        CourthousePatch courthousePatch = someCourtHousePatchForRegion(2);
        assertThatThrownBy(() -> validator.validate(courthousePatch, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", CourthouseApiError.REGION_ID_DOES_NOT_EXIST);
    }

    @Test
    void throwsWhenSecurityGroupDoesntExist() {
        when(courthouseRepository.findById(1)).thenReturn(Optional.of(someCourthouse()));
        when(securityGroupRepository.existsAllByIdIn(Set.of(2))).thenReturn(false);

        CourthousePatch courthousePatch = someCourtHousePatchForSecurityGroup(2);
        assertThatThrownBy(() -> validator.validate(courthousePatch, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", SECURITY_GROUP_ID_DOES_NOT_EXIST);
    }

    @Test
    void doesntThrowGivenValidPatch() {
        var targetCourthouse = someCourthouse();
        when(courthouseRepository.findById(1)).thenReturn(Optional.of(targetCourthouse));
        when(courthouseRepository.existsByCourthouseNameAndIdNot("SOME-NAME", 1)).thenReturn(false);
        when(courthouseRepository.existsByDisplayNameIgnoreCaseAndIdNot("some-display-name", 1)).thenReturn(false);
        when(securityGroupRepository.existsAllByIdIn(Set.of(1, 2, 3))).thenReturn(true);
        when(regionRepository.existsById(1)).thenReturn(true);

        validator.validate(someValidCourthousePatch(), 1);

        assertThatNoException().isThrownBy(() -> validator.validate(someValidCourthousePatch(), 1));
    }

    private CourthousePatch someValidCourthousePatch() {
        return new CourthousePatch()
            .regionId(1)
            .courthouseName("some-name")
            .displayName("some-display-name")
            .securityGroupIds(List.of(1, 2, 3));
    }

    private CourthouseEntity someCourthouse() {
        var courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("some-name");
        return courthouse;
    }

    private CourthousePatch someCourtHousePatchForRegion(Integer regionId) {
        return new CourthousePatch().regionId(regionId);
    }

    private CourthousePatch someCourtHousePatchForSecurityGroup(Integer securityGroup) {
        return new CourthousePatch().securityGroupIds(List.of(securityGroup));
    }

    private CourthousePatch someCourthousePatchWithCourthouseName(String name) {
        return new CourthousePatch().courthouseName(name);
    }

    private CourthousePatch someCourthousePatchWithCourthouseDisplayName(String name) {
        return new CourthousePatch().displayName(name);
    }

}
