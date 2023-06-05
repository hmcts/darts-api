package uk.gov.hmcts.darts.courthouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CourthouseServiceImplTest {

    public static final String TEST_COURTHOUSE_NAME = "Test courthouse";
    public static final short CODE = (short) 123;

    @InjectMocks
    CourthouseServiceImpl courthouseService;

    @Mock
    CourthouseRepository repository;

    @Mock
    CourthouseToCourthouseEntityMapper mapper;

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
}
