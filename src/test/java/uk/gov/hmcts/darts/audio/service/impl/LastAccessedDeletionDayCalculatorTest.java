package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest(classes = LastAccessedDeletionDayCalculator.class)
@ActiveProfiles({"dev", "h2db"})
class LastAccessedDeletionDayCalculatorTest {
    @MockBean
    private BankHolidaysService bankHolidaysService;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;

    @BeforeEach
    void setUp() {
        //setting clock to 2023-10-27
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void whereLastAccessedDoesNotIncludesNonBusinessDays() {
        //setting clock to 2023-10-23 on a monday.
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 23, 22, 0, 0, 0, ZoneOffset.UTC));
        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(Collections.emptyList());

        assertEquals(
            OffsetDateTime.of(2023, 10, 19, 22, 0, 0, 0, ZoneOffset.UTC),
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
            OffsetDateTime.of(2023, 10, 20, 22, 0, 0, 0, ZoneOffset.UTC),
            lastAccessedDeletionDayCalculator.getStartDateForDeletion(2)
        );

    }

    @Test
    void getStartDateWithChangedDefault() {
        assertEquals(
            OffsetDateTime.of(2023, 10, 23, 22, 0, 0, 0, ZoneOffset.UTC),
            this.lastAccessedDeletionDayCalculator.getStartDateForDeletion(4)
        );
    }

    @Test
    void whereLastAccessedDoesIncludesNonBusinessDaysAndChristmas() {
        /*
        setting clock to 2023-12-28
        2023-12-27 - normal day
        2023-12-26 - holiday
        2023-12-25 - holiday
        2023-12-24 - weekend
        2023-12-23 - weekend
        2023-12-22 - normal day
         */
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 12, 28, 22, 0, 0, 0, ZoneOffset.UTC));

        List<LocalDate> holidays = new ArrayList<>();
        holidays.add(LocalDate.of(2023, Month.DECEMBER, 26));
        holidays.add(LocalDate.of(2023, Month.DECEMBER, 25));

        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(holidays);

        assertEquals(
            OffsetDateTime.of(2023, 12, 22, 22, 0, 0, 0, ZoneOffset.UTC),
            lastAccessedDeletionDayCalculator.getStartDateForDeletion(2)
        );
    }

    @Test
    void whereLastAccessedOverChristmasNewYears() {
        /*
        setting clock to 2024-01-02
        2024-01-02 - normal day
        2024-01-01 - holiday
        2023-12-31 - weekend
        2023-12-30 - weekend
        2023-12-29 - normal day
        2023-12-28 - normal day
        2023-12-27 - normal day
        2023-12-26 - holiday
        2023-12-25 - holiday
        2023-12-24 - weekend
        2023-12-23 - weekend
        2023-12-22 - normal day
         */
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2024, 1, 2, 22, 0, 0, 0, ZoneOffset.UTC));

        List<LocalDate> holidays = new ArrayList<>();
        holidays.add(LocalDate.of(2024, Month.JANUARY, 1));
        holidays.add(LocalDate.of(2023, Month.DECEMBER, 26));
        holidays.add(LocalDate.of(2023, Month.DECEMBER, 25));

        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(holidays);

        assertEquals(
            OffsetDateTime.of(2023, 12, 14, 22, 0, 0, 0, ZoneOffset.UTC),
            lastAccessedDeletionDayCalculator.getStartDateForDeletion(10)
        );
    }


}


