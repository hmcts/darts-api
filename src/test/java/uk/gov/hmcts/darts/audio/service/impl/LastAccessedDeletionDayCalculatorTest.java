package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
class LastAccessedDeletionDayCalculatorTest {
    @MockBean
    private BankHolidaysService bankHolidaysService;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;

    @BeforeEach
    void setUp() {
        //setting clock to 2023-10-27
        when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.of(2023, 10, 27));
    }

    @Test
    void whereLastAccessedDoesNotIncludesNonBusinessDays() {
        //setting clock to 2023-10-23 on a monday
        when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.of(2023, 10, 23));
        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(Collections.emptyList());
        //when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.of(2024, 1, 1));

        assertEquals(
            OffsetDateTime.of(2023, 10, 19, 0, 0, 0, 0, ZoneOffset.UTC),
            lastAccessedDeletionDayCalculator.getStartDateForDeletion(2)
        );
    }

    @Test
    void getStartDateWith3BankHoliday() {
        List<LocalDate> holidays = new ArrayList<>();
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 26));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 25));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 24));

        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(holidays);

        assertEquals(
            OffsetDateTime.of(2023, 10, 20, 0, 0, 0, 0, ZoneOffset.UTC),
            lastAccessedDeletionDayCalculator.getStartDateForDeletion(2)
        );

    }

    @Test
    void getStartDateWithChangedDefault() {
        assertEquals(
            OffsetDateTime.of(2023, 10, 23, 0, 0, 0, 0, ZoneOffset.UTC),
            this.lastAccessedDeletionDayCalculator.getStartDateForDeletion(4)
        );
    }

}


