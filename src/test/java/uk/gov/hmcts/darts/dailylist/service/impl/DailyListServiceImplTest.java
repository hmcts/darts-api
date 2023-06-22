package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.courthouse.api.CourthouseApi;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
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

    @BeforeAll
    void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void processIncomingDailyListOkWhenCodeNotMatchExceptionThrown() throws IOException, CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        Courthouse entity = new Courthouse();
        entity.setCourthouseName("SWANSEA");
        entity.setCode(457);
        CourthouseCodeNotMatchException exception = new CourthouseCodeNotMatchException(entity, 457, "test");

        when(courthouseApi.retrieveAndUpdateCourtHouse(anyInt(), anyString())).thenThrow(exception);
        when(dailyListRepository.findByUniqueId(anyString())).thenReturn(Optional.empty());
        String requestBody = getContentsFromFile(
            "Tests/dailylist/DailyListServiceImplTest/processIncomingDailyList/DailyListRequest.json");
        DailyList dailyList = objectMapper.readValue(requestBody, DailyList.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, dailyList);
        service.processIncomingDailyList(request);
        verify(dailyListRepository).saveAndFlush(any(DailyListEntity.class));

    }
}
