package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.courthouse.api.CourthouseApi;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyList;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DailyListServiceImplTest {
    public static final String CPP = "CPP";
    ObjectMapper objectMapper;

    @InjectMocks
    DailyListServiceImpl service;

    @Mock
    CourthouseApi courthouseApi;

    @Mock
    DailyListRepository dailyListRepository;

    @Mock
    DailyListMapper dailyListMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void processIncomingDailyListOkWhenCodeNotMatchExceptionThrown() throws IOException, CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Courthouse entity = new Courthouse();
        entity.setCourthouseName("SWANSEA");
        entity.setCode(457);
        CourthouseCodeNotMatchException exception = new CourthouseCodeNotMatchException(entity, 457, "test");

        when(courthouseApi.retrieveAndUpdateCourtHouse(anyInt(), anyString())).thenThrow(exception);
        when(dailyListRepository.findByUniqueId(anyString())).thenReturn(Optional.empty());
        when(dailyListMapper.mapToDailyListEntity(any(DailyListPostRequest.class), any(Courthouse.class))).thenReturn(new DailyListEntity());
        String requestBody = getContentsFromFile(
            "Tests/dailylist/DailyListServiceImplTest/processIncomingDailyList/DailyListRequest.json");
        DailyList dailyList = objectMapper.readValue(requestBody, DailyList.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, dailyList);
        service.processIncomingDailyList(request);

        //make sure an exception is not thrown.
        verify(dailyListRepository).saveAndFlush(any(DailyListEntity.class));

    }
}
