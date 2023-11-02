package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LastAccessedDeletionDayCalculatorTest {
    public static final LocalDate DATE_27TH_OCTOBER = LocalDate.of(2023, Month.OCTOBER, 27);
    @Mock
    BankHolidaysService bankHolidaysService;
    LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;
    private Clock clock;

    @BeforeEach
    void setUp() {
        //setting clock to 2023-10-27 13:56:17 with local offset on a friday
        this.clock = Clock.fixed(Instant.ofEpochSecond(1_698_414_977L), ZoneId.of("Europe/London"));
        this.lastAccessedDeletionDayCalculator = new LastAccessedDeletionDayCalculator(bankHolidaysService, clock, 2);
    }

    @Test
    void whereLastAccessedDoesNotIncludesNonBusinessDays() {
        //setting clock to 2023-10-23 on a monday
        this.lastAccessedDeletionDayCalculator = new LastAccessedDeletionDayCalculator(
            bankHolidaysService, Clock.fixed(Instant.ofEpochSecond(1_698_019_200L), ZoneId.of("Europe/London")),
            2
        );

        assertEquals(
            OffsetDateTime.of(2023, 10, 19, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
            this.lastAccessedDeletionDayCalculator.getStartDateForDeletion()
        );
    }

    @Test
    void getStartDateWith3BankHoliday() {
        Event bankHoliday1 = new Event();
        bankHoliday1.setDate(DATE_27TH_OCTOBER.minusDays(1));

        Event bankHoliday2 = new Event();
        bankHoliday2.setDate(DATE_27TH_OCTOBER.minusDays(2));

        Event bankHoliday3 = new Event();
        bankHoliday3.setDate(DATE_27TH_OCTOBER.minusDays(3));

        when(bankHolidaysService.getBankHolidaysFor(anyInt())).thenReturn(List.of(
            bankHoliday1,
            bankHoliday2,
            bankHoliday3
        ));

        assertEquals(
            OffsetDateTime.of(2023, 10, 23, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
            this.lastAccessedDeletionDayCalculator.getStartDateForDeletion()
        );

    }

    @Test
    void getStartDateWithChangedDefault() {
        this.lastAccessedDeletionDayCalculator = new LastAccessedDeletionDayCalculator(bankHolidaysService, clock, 4);

        assertEquals(
            OffsetDateTime.of(2023, 10, 23, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
            this.lastAccessedDeletionDayCalculator.getStartDateForDeletion()
        );
    }

}


