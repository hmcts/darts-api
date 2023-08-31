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
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.courthouses.CourthouseRepository;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouses.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouses.service.CourthouseServiceImpl;

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
    CourthouseRepository repository;

    @Mock
    CourthouseToCourthouseEntityMapper mapper;

    @Captor
    ArgumentCaptor<Integer> captorInteger;

    @Test
    void testAddCourtHouse() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        uk.gov.hmcts.darts.courthouses.model.Courthouse courthouseModel = new uk.gov.hmcts.darts.courthouses.model.Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(mapper.mapToEntity(courthouseModel)).thenReturn(courthouseEntity);
        Mockito.when(repository.saveAndFlush(courthouseEntity)).thenReturn(courthouseEntity);


        CourthouseEntity returnedEntity = courthouseService.addCourtHouse(courthouseModel);
        assertEquals("Test courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 123, returnedEntity.getCode());
    }

    @Test
    void addDuplicateCourtHouseName() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        uk.gov.hmcts.darts.courthouses.model.Courthouse courthouseModel = new uk.gov.hmcts.darts.courthouses.model.Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(repository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(Optional.of(new CourthouseEntity()));

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

        uk.gov.hmcts.darts.courthouses.model.Courthouse courthouseModel = new uk.gov.hmcts.darts.courthouses.model.Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(repository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        Mockito.when(repository.findByCode(any(Integer.class))).thenReturn(Optional.of(new CourthouseEntity()));

        var exception = assertThrows(
            DartsApiException.class,
            () -> courthouseService.addCourtHouse(courthouseModel)
        );

        assertEquals("Provided courthouse code already exists.", exception.getMessage());
    }

    @Test
    void testDeleteCourthouseById() {
        Mockito.doNothing().when(repository).deleteById(COURTHOUSE_ID);

        courthouseService.deleteCourthouseById(COURTHOUSE_ID);

        Mockito.verify(repository).deleteById(captorInteger.capture());
        assertEquals(COURTHOUSE_ID, captorInteger.getValue());
    }

    @Test
    void testAmendCourthouseById() {
        CourthouseEntity courthouseEntityOriginal = new CourthouseEntity();
        courthouseEntityOriginal.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntityOriginal.setCode(CODE);

        uk.gov.hmcts.darts.courthouses.model.Courthouse courthouseModelAmendment = new uk.gov.hmcts.darts.courthouses.model.Courthouse();
        courthouseModelAmendment.setCourthouseName("Changed courthouse");
        courthouseModelAmendment.setCode(543);

        CourthouseEntity courthouseEntityChanged = new CourthouseEntity();
        courthouseEntityChanged.setCourthouseName("Changed courthouse");
        courthouseEntityChanged.setCode(543);


        Mockito.when(repository.getReferenceById(COURTHOUSE_ID)).thenReturn(courthouseEntityOriginal);
        Mockito.when(repository.saveAndFlush(any())).thenReturn(courthouseEntityChanged);

        CourthouseEntity returnedEntity = courthouseService.amendCourthouseById(
            courthouseModelAmendment,
            COURTHOUSE_ID
        );


        assertEquals("Changed courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 543, returnedEntity.getCode());

    }

    @Test
    void testGetCourtHouseByIdTest() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Mockito.when(repository.getReferenceById(anyInt())).thenReturn(courthouseEntity);


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
        Mockito.when(repository.findAll()).thenReturn(courthouseList);


        List<CourthouseEntity> returnedEntities = courthouseService.getAllCourthouses();
        assertEquals(2, returnedEntities.size());
    }

    @Test
    void retrieveCourthouseUsingJustName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Mockito.when(repository.findByCourthouseNameIgnoreCase(SWANSEA_NAME_UC)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(null, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingCodeAndName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Mockito.when(repository.findByCode(SWANSEA_CODE)).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        CourthouseEntity courthouse = courthouseService.retrieveAndUpdateCourtHouse(SWANSEA_CODE, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(SWANSEA_CODE, courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingNameAndDifferentCode() {
        Mockito.when(repository.findByCode(458)).thenReturn(Optional.empty());
        Mockito.when(repository.findByCourthouseNameIgnoreCase(SWANSEA_NAME_UC)).thenReturn(Optional.of(
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
        Mockito.when(repository.findByCode(Short.parseShort("458"))).thenReturn(Optional.empty());
        Mockito.when(repository.findByCourthouseNameIgnoreCase("TEST")).thenReturn(Optional.empty());

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


}
