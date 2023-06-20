package uk.gov.hmcts.darts.courthouse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class CourthouseServiceImplTest {

    public static final String TEST_COURTHOUSE_NAME = "Test courthouse";
    public static final short CODE = 123;
    public static final int COURTHOUSE_ID = 11;
    public static final String SWANSEA_NAME = "swansea";
    public static final int SWANSEA_CODE = 457;
    public static final String SWANSEA_CODE_STRING = "457";
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
        Courthouse courthouseEntity = new Courthouse();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        uk.gov.hmcts.darts.courthouse.model.Courthouse courthouseModel = new uk.gov.hmcts.darts.courthouse.model.Courthouse();
        courthouseModel.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseModel.setCode((int) CODE);


        Mockito.when(mapper.mapToEntity(courthouseModel)).thenReturn(courthouseEntity);
        Mockito.when(repository.saveAndFlush(courthouseEntity)).thenReturn(courthouseEntity);


        Courthouse returnedEntity = courthouseService.addCourtHouse(courthouseModel);
        assertEquals("Test courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 123, returnedEntity.getCode());
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
        Courthouse courthouseEntityOriginal = new Courthouse();
        courthouseEntityOriginal.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntityOriginal.setCode(CODE);

        uk.gov.hmcts.darts.courthouse.model.Courthouse courthouseModelAmendment = new uk.gov.hmcts.darts.courthouse.model.Courthouse();
        courthouseModelAmendment.setCourthouseName("Changed courthouse");
        courthouseModelAmendment.setCode(543);

        Courthouse courthouseEntityChanged = new Courthouse();
        courthouseEntityChanged.setCourthouseName("Changed courthouse");
        courthouseEntityChanged.setCode((short) 543);


        Mockito.when(repository.getReferenceById(COURTHOUSE_ID)).thenReturn(courthouseEntityOriginal);
        Mockito.when(repository.saveAndFlush(any())).thenReturn(courthouseEntityChanged);

        Courthouse returnedEntity = courthouseService.amendCourthouseById(courthouseModelAmendment, COURTHOUSE_ID);


        assertEquals("Changed courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 543, returnedEntity.getCode());

    }

    @Test
    void testGetCourtHouseByIdTest() {
        Courthouse courthouseEntity = new Courthouse();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Mockito.when(repository.getReferenceById(anyInt())).thenReturn(courthouseEntity);


        Courthouse returnedEntity = courthouseService.getCourtHouseById(COURTHOUSE_ID);
        assertEquals("Test courthouse", returnedEntity.getCourthouseName());
        assertEquals((short) 123, returnedEntity.getCode());
    }

    @Test
    void testGetAllCourthouses() {
        Courthouse courthouseEntity = new Courthouse();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Courthouse courthouseEntity2 = new Courthouse();
        courthouseEntity2.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity2.setCode(CODE);

        List<Courthouse> courthouseList = Arrays.asList(courthouseEntity, courthouseEntity2);
        Mockito.when(repository.findAll()).thenReturn(courthouseList);


        List<Courthouse> returnedEntities = courthouseService.getAllCourthouses();
        assertEquals(2, returnedEntities.size());
    }

    @Test
    void retrieveCourthouseUsingJustName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Mockito.when(repository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(createSwanseaCourthouseEntity()));
        Courthouse courthouse = courthouseService.retrieveCourtHouse(null, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(Short.parseShort(SWANSEA_CODE_STRING), courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingCodeAndName() throws CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Mockito.when(repository.findByCode(Short.parseShort(SWANSEA_CODE_STRING))).thenReturn(Optional.of(
            createSwanseaCourthouseEntity()));
        Courthouse courthouse = courthouseService.retrieveCourtHouse(SWANSEA_CODE, SWANSEA_NAME);
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(Short.parseShort(SWANSEA_CODE_STRING), courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingNameAndDifferentCode() {
        Mockito.when(repository.findByCode(Short.parseShort("458"))).thenReturn(Optional.empty());
        Mockito.when(repository.findByCourthouseName(SWANSEA_NAME_UC)).thenReturn(Optional.of(createSwanseaCourthouseEntity()));

        CourthouseCodeNotMatchException thrownException = assertThrows(
            CourthouseCodeNotMatchException.class,
            () -> courthouseService.retrieveCourtHouse(458, SWANSEA_NAME)
        );

        Courthouse courthouse = thrownException.getCourthouse();
        assertEquals(SWANSEA_NAME_UC, courthouse.getCourthouseName());
        assertEquals(Short.parseShort(SWANSEA_CODE_STRING), courthouse.getCode());
    }

    @Test
    void retrieveCourthouseUsingInvalidName() {
        Mockito.when(repository.findByCode(Short.parseShort("458"))).thenReturn(Optional.empty());
        Mockito.when(repository.findByCourthouseName("TEST")).thenReturn(Optional.empty());

        assertThrows(
            CourthouseNameNotFoundException.class,
            () -> courthouseService.retrieveCourtHouse(458, "test")
        );

    }

    private Courthouse createSwanseaCourthouseEntity() {
        Courthouse courthouseEntity = new Courthouse();
        courthouseEntity.setCourthouseName(SWANSEA_NAME_UC);
        courthouseEntity.setCode(Short.parseShort(SWANSEA_CODE_STRING));
        return courthouseEntity;
    }


}
