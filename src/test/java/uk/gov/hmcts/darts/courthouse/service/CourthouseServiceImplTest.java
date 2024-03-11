package uk.gov.hmcts.darts.courthouse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.AdminRegionToRegionEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class CourthouseServiceImplTest {

    public static final String TEST_COURTHOUSE_NAME = "Test courthouse";
    public static final int CODE = 123;
    public static final int COURTHOUSE_ID = 11;
    public static final String SWANSEA_NAME = "swansea";
    public static final int SWANSEA_CODE = 457;
    public static final String SWANSEA_NAME_UC = "SWANSEA";

    @InjectMocks
    CourthouseServiceImpl courthouseService;

    @Mock
    CourthouseRepository courthouseRepository;

    @Mock
    CourthouseToCourthouseEntityMapper courthouseMapper;

    @Mock
    RegionRepository regionRepository;

    @Mock
    AdminRegionToRegionEntityMapper regionMapper;

    @Captor
    ArgumentCaptor<Integer> captorInteger;

    @Test
    void testAddCourtHouse() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Courthouse courthouseModel = new Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(courthouseMapper.mapToEntity(courthouseModel)).thenReturn(courthouseEntity);
        Mockito.when(courthouseRepository.saveAndFlush(courthouseEntity)).thenReturn(courthouseEntity);


        CourthouseEntity returnedEntity = courthouseService.addCourtHouse(courthouseModel);
        assertEquals("Test courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 123, returnedEntity.getCode());
    }

    @Test
    void addDuplicateCourtHouseName() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Courthouse courthouseModel = new Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(Optional.of(new CourthouseEntity()));

        var exception = assertThrows(
            DartsApiException.class,
            () -> courthouseService.addCourtHouse(courthouseModel)
        );

        assertEquals("Provided courthouse name already exists.", exception.getMessage());
    }

    @Test
    void addDuplicateCourtHouseCode() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Courthouse courthouseModel = new Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        Mockito.when(courthouseRepository.findByCode(any(Integer.class))).thenReturn(Optional.of(new CourthouseEntity()));

        var exception = assertThrows(
            DartsApiException.class,
            () -> courthouseService.addCourtHouse(courthouseModel)
        );

        assertEquals("Provided courthouse code already exists.", exception.getMessage());
    }

    @Test
    void testDeleteCourthouseById() {
        Mockito.doNothing().when(courthouseRepository).deleteById(COURTHOUSE_ID);

        courthouseService.deleteCourthouseById(COURTHOUSE_ID);

        Mockito.verify(courthouseRepository).deleteById(captorInteger.capture());
        assertEquals(COURTHOUSE_ID, captorInteger.getValue());
    }

    @Test
    void testGetCourtHouseByIdTest() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Mockito.when(courthouseRepository.getReferenceById(anyInt())).thenReturn(courthouseEntity);


        CourthouseEntity returnedEntity = courthouseService.getCourtHouseById(COURTHOUSE_ID);
        assertEquals("Test courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 123, returnedEntity.getCode());
    }

    @Test
    void testGetAllCourthouses() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        CourthouseEntity courthouseEntity2 = new CourthouseEntity();
        courthouseEntity2.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity2.setCode(CODE);

        List<CourthouseEntity> courthouseList = Arrays.asList(courthouseEntity, courthouseEntity2);
        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouseList);


        List<CourthouseEntity> returnedEntities = courthouseService.getAllCourthouses();
        assertEquals(2, returnedEntities.size());
    }

    @Test
    void retrieveCourthouseUsingJustName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA_NAME_UC)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(null, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingCodeAndName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Mockito.when(courthouseRepository.findByCode(SWANSEA_CODE)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(SWANSEA_CODE, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingNameAndDifferentCode() {
        Mockito.when(courthouseRepository.findByCode(458)).thenReturn(Optional.empty());
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA_NAME_UC)).thenReturn(Optional.of(
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
    void retrieveCourthouseUsingInvalidName() {
        Mockito.when(courthouseRepository.findByCode(Short.parseShort("458"))).thenReturn(Optional.empty());
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase("TEST")).thenReturn(Optional.empty());

        assertThrows(
            CourthouseNameNotFoundException.class,
            () -> courthouseService.retrieveAndUpdateCourtHouse(458, "test")
        );

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
        Mockito.when(regionRepository.findAll()).thenReturn(regions);

        List<RegionEntity> returnedEntities = courthouseService.getAdminAllRegions();
        assertEquals(2, returnedEntities.size());
        assertEquals(regionEntity1, returnedEntities.get(0));
        assertEquals(regionEntity2, returnedEntities.get(1));
    }

}
