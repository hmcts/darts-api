package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.event.model.CourtLog;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtLogsServiceImplTest {

    private static final String CASE_0000001 = "Case0000001";
    @InjectMocks
    private CourtLogsServiceImpl courtLogsService;

    @Mock
    private CourtLogEventRepository repository;

    @Test
    void testGetCourtLogs() {

        var hearingEntity = CommonTestDataUtil.createHearing(CASE_0000001, LocalTime.of(10, 0));
        List<EventEntity> event = List.of(CommonTestDataUtil.createEventWith("LOG", "Test", hearingEntity));

        when(repository.findByCourthouseAndCaseNumberBetweenStartAndEnd(
            "SWANSEA",
            CASE_0000001,
            CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:00:00"),
            CommonTestDataUtil.createOffsetDateTime("2023-07-01T12:00:00")
        )).thenReturn(event);

        List<CourtLog> entities = courtLogsService.getCourtLogs(
            "SWANSEA",
            CASE_0000001,
            CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:00:00"),
            CommonTestDataUtil.createOffsetDateTime("2023-07-01T12:00:00")
        );

        assertEquals("SWANSEA", entities.get(0).getCourthouse());
        assertEquals(CASE_0000001, entities.get(0).getCaseNumber());
        assertEquals("2023-07-01T10:00Z", entities.get(0).getTimestamp().toString());
        assertEquals("Test", entities.get(0).getEventText());

    }


}
