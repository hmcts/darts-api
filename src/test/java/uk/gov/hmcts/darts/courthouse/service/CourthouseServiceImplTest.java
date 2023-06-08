package uk.gov.hmcts.darts.courthouse.service;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class CourthouseServiceImplTest {

    public static final String TEST_COURTHOUSE_NAME = "Test courthouse";
    public static final Integer CODE = 123;
    public static final int COURTHOUSE_ID = 11;

    @InjectMocks
    CourthouseServiceImpl courthouseService;

    @Mock
    CourthouseRepository repository;

    @Mock
    CourthouseToCourthouseEntityMapper mapper;

    @Captor
    ArgumentCaptor<Integer> captorInteger;


    @BeforeEach
    void setUp() {
    }


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
        assertEquals(123, returnedEntity.getCode());
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
        courthouseEntityChanged.setCode(543);


        Mockito.when(repository.getReferenceById(COURTHOUSE_ID)).thenReturn(courthouseEntityOriginal);
        Mockito.when(repository.saveAndFlush(any())).thenReturn(courthouseEntityChanged);

        Courthouse returnedEntity = courthouseService.amendCourthouseById(courthouseModelAmendment, COURTHOUSE_ID);



        assertEquals("Changed courthouse", returnedEntity.getCourthouseName());
        assertEquals(543, returnedEntity.getCode());

    }

    @Test
    void testGetCourtHouseByIdTest() {
        Courthouse courthouseEntity = new Courthouse();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Mockito.when(repository.getReferenceById(anyInt())).thenReturn(courthouseEntity);


        Courthouse returnedEntity = courthouseService.getCourtHouseById(COURTHOUSE_ID);
        assertEquals("Test courthouse", returnedEntity.getCourthouseName());
        assertEquals(123, returnedEntity.getCode());
    }

    @Test
    void testGetAllCourthouses() {
        Courthouse courthouseEntity = new Courthouse();
        courthouseEntity.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity.setCode(CODE);

        Courthouse courthouseEntity2 = new Courthouse();
        courthouseEntity2.setCourthouseName(TEST_COURTHOUSE_NAME);
        courthouseEntity2.setCode(CODE);

        List<Courthouse> courthouseList = Arrays.asList(courthouseEntity,courthouseEntity2);
        Mockito.when(repository.findAll()).thenReturn(courthouseList);


        List<Courthouse> returnedEntities = courthouseService.getAllCourthouses();
        assertEquals(2, returnedEntities.size());
    }
}
