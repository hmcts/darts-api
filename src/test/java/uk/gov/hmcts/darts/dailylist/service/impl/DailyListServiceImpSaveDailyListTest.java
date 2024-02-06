package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
class DailyListServiceImpSaveDailyListTest {

    public static final String CPP = "CPP";
    ObjectMapper objectMapper;

    @InjectMocks
    DailyListServiceImpl service;

    @Mock
    DailyListRepository dailyListRepository;

    @Mock
    DailyListMapper dailyListMapper;

    @Captor
    private ArgumentCaptor<DailyListEntity> dailyListEntityArgumentCaptor;


    @BeforeEach
    void beforeEach() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void ok_WhenCodeNotMatchExceptionThrown() throws IOException, CourthouseCodeNotMatchException, CourthouseNameNotFoundException {
        when(dailyListRepository.findByUniqueId(anyString())).thenReturn(Optional.empty());
        when(dailyListMapper.createDailyListEntity(
              any(DailyListPostRequest.class),
              any(String.class)
        )).thenReturn(new DailyListEntity());
        String dailyListJson = getContentsFromFile(
              "Tests/dailylist/DailyListServiceImplTest/processIncomingDailyList/DailyListRequest.json");
        DailyListJsonObject dailyList = objectMapper.readValue(dailyListJson, DailyListJsonObject.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, null, null, null, null, null, dailyList);
        service.saveDailyListToDatabase(request);

        //make sure an exception is not thrown.
        verify(dailyListRepository).saveAndFlush(any(DailyListEntity.class));
    }


    @Test
    void ok_Xml() {
        when(dailyListRepository.findByUniqueId(anyString())).thenReturn(Optional.empty());

        DailyListPostRequest request = new DailyListPostRequest(CPP, "Swansea", LocalDate.now(), "Thexml",
                                                                "uniqueId",
                                                                OffsetDateTime.now(),
                                                                null
        );
        service.saveDailyListToDatabase(request);

        verify(dailyListRepository).saveAndFlush(dailyListEntityArgumentCaptor.capture());

        DailyListEntity savedDailyList = dailyListEntityArgumentCaptor.getValue();
        assertThat(savedDailyList.getUniqueId()).isEqualTo("uniqueId");
        assertThat(savedDailyList.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(savedDailyList.getSource()).isEqualTo("CPP");
        assertThat(savedDailyList.getListingCourthouse()).isNotNull();
        assertThat(savedDailyList.getXmlContent()).isEqualTo("Thexml");
        assertThat(savedDailyList.getContent()).isNull();
    }

}
