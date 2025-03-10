package uk.gov.hmcts.darts.courthouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.AdminCourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.AdminRegionToRegionEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.service.impl.CourthouseServiceImpl;
import uk.gov.hmcts.darts.courthouse.validation.CourthousePatchValidator;
import uk.gov.hmcts.darts.usermanagement.api.UserManagementApi;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourthouseServiceImplTest {

    private static final String TEST_COURTHOUSE_NAME = "Test courthouse";
    private static final int CODE = 123;
    private static final int COURTHOUSE_ID = 11;
    private static final String SWANSEA_NAME = "swansea";
    private static final int SWANSEA_CODE = 457;
    private static final String SWANSEA_NAME_UC = "SWANSEA";

    private CourthouseServiceImpl courthouseService;

    @Mock
    private CourthouseRepository courthouseRepository;

    @Mock
    private CourthouseToCourthouseEntityMapper courthouseMapper;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private AdminRegionToRegionEntityMapper regionMapper;

    @Mock
    private CourthousePatchValidator courthousePatchValidator;

    @Mock
    private CourthouseUpdateMapper courthouseUpdateMapper;

    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private AuditApi auditApi;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private SecurityRoleRepository securityRoleRepository;
    @Mock
    private AdminCourthouseToCourthouseEntityMapper adminMapper;
    @Mock
    private UserManagementApi userManagementApi;

    @BeforeEach
    void setup() {
        courthouseService = new CourthouseServiceImpl(courthouseRepository,
                                                      hearingRepository,
                                                      caseRepository,
                                                      regionRepository,
                                                      securityGroupRepository,
                                                      securityRoleRepository,
                                                      adminMapper,
                                                      courthouseMapper,
                                                      courthouseUpdateMapper,
                                                      userManagementApi,
                                                      courthousePatchValidator,
                                                      authorisationApi,
                                                      auditApi);
    }

    @Test
    void testGetCourtHouseByIdTest() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        when(courthouseRepository.getReferenceById(anyInt())).thenReturn(courthouseEntity);


        CourthouseEntity returnedEntity = courthouseService.getCourtHouseById(COURTHOUSE_ID);
        assertEquals("TEST COURTHOUSE", returnedEntity.getCourthouseName());
        assertEquals((short) 123, returnedEntity.getCode());
    }

    @Test
    void testGetAllCourthouses() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);
        courthouseEntity.setId(1);

        CourthouseEntity courthouseEntity2 = new CourthouseEntity();
        courthouseEntity2.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity2.setCode(CODE);
        courthouseEntity2.setId(2);

        List<Integer> courthouseIds = List.of(courthouseEntity.getId(), courthouseEntity2.getId());
        when(authorisationApi.getListOfCourthouseIdsUserHasAccessTo()).thenReturn(courthouseIds);

        List<CourthouseEntity> courthouseList = Arrays.asList(courthouseEntity, courthouseEntity2);
        when(courthouseRepository.findByIdIn(courthouseIds)).thenReturn(courthouseList);

        List<CourthouseEntity> returnedEntities = courthouseService.getAllCourthouses();
        assertEquals(2, returnedEntities.size());
    }


    @Test
    void retrieveCourthouse_UsingJustName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        when(courthouseRepository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(null, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouse_UsingCodeAndName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        when(courthouseRepository.findByCode(SWANSEA_CODE)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(SWANSEA_CODE, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouse_RetrievesCourthouse_UsingWhitespaceCourthouseName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        // Given
        when(courthouseRepository.findByCode(SWANSEA_CODE)).thenReturn(Optional.empty());
        when(courthouseRepository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(createSwanseaCourthouseEntity()));

        // When
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(SWANSEA_CODE, " Swansea ");

        // Then
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouse_RetrievesCourthouse_UsingWhitespaceCourthouseNameAndNullCode()
        throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        // Given
        when(courthouseRepository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(createSwanseaCourthouseEntity()));

        // When
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(null, " Swansea ");

        // Then
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }


    @Test
    void retrieveCourthouse_UsingNameAndDifferentCode() {
        when(courthouseRepository.findByCode(458)).thenReturn(Optional.empty());
        when(courthouseRepository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));

        CourthouseCodeNotMatchException thrownException = assertThrows(
            CourthouseCodeNotMatchException.class,
            () -> courthouseService.retrieveAndUpdateCourtHouse(458, SWANSEA_NAME)
        );

        CourthouseEntity courthouse = thrownException.getDatabaseCourthouse();
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouse_UsingInvalidName() {
        when(courthouseRepository.findByCode(Short.parseShort("458"))).thenReturn(Optional.empty());
        when(courthouseRepository.findByCourthouseName("TEST")).thenReturn(Optional.empty());

        assertThrows(
            CourthouseNameNotFoundException.class,
            () -> courthouseService.retrieveAndUpdateCourtHouse(458, "test")
        );

    }

    @Test
    void retrieveCourthouse_WithExistingNullCode() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        // given
        when(courthouseRepository.findByCode(Short.parseShort("458"))).thenReturn(Optional.empty());
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(SWANSEA_NAME_UC);
        courthouseEntity.setCode(null);
        when(courthouseRepository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(courthouseEntity));

        // when
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(458, SWANSEA_NAME);

        // then
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(458, courthouse.getCode());
    }

    private CourthouseEntity createSwanseaCourthouseEntity() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(SWANSEA_NAME_UC);
        courthouseEntity.setCode(SWANSEA_CODE);
        return courthouseEntity;
    }

    @Test
    void testGetAllRegions() {
        RegionEntity regionEntity1 = new RegionEntity();
        regionEntity1.setId(1);
        regionEntity1.setRegionName("South");
        RegionEntity regionEntity2 = new RegionEntity();
        regionEntity2.setId(2);
        regionEntity2.setRegionName("North");

        List<RegionEntity> regions = Arrays.asList(regionEntity1, regionEntity2);
        when(regionRepository.findAll()).thenReturn(regions);

        List<RegionEntity> returnedEntities = courthouseService.getAdminAllRegions();
        assertEquals(2, returnedEntities.size());
        assertEquals(regionEntity1, returnedEntities.getFirst());
        assertEquals(regionEntity2, returnedEntities.get(1));
    }

    @Test
    void validatesPatch() {
        when(courthouseRepository.findById(COURTHOUSE_ID)).thenReturn(Optional.of(someCourthouse()));

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);

        when(courthouseUpdateMapper.mapPatchToEntity(any(CourthousePatch.class), any(CourthouseEntity.class))).thenReturn(new CourthouseEntity());
        var courthousePatch = new CourthousePatch();
        courthouseService.updateCourthouse(COURTHOUSE_ID, courthousePatch);

        verify(courthousePatchValidator, times(1)).validate(courthousePatch, COURTHOUSE_ID);
    }

    private CourthouseEntity someCourthouse() {
        return new CourthouseEntity();
    }

}
