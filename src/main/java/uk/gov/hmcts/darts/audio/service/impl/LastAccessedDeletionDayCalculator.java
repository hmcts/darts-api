package uk.gov.hmcts.darts.audio.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class LastAccessedDeletionDayCalculator {

    private final BankHolidaysService bankHolidaysService;
    private final Clock clock;

    private final long deletionDays;

    public LastAccessedDeletionDayCalculator(BankHolidaysService bankHolidaysService,
                                             Clock clock, @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}") long deletionDays) {
        this.bankHolidaysService = bankHolidaysService;
        this.clock = clock;
        this.deletionDays = deletionDays;
    }

    private static long calculateWeekendDays(LocalDate fromDate, LocalDate toDate) {
        Set<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        return fromDate.datesUntil(toDate)
            .filter(d -> weekend.contains(d.getDayOfWeek()))
            .count();
    }

    private static boolean isBankHolidayBetweenDates(Event bankHoliday, LocalDate cutOff, LocalDate today) {
        return !bankHoliday.getDate().isBefore(cutOff) && !bankHoliday.getDate().isAfter(today);
    }

    public OffsetDateTime getStartDateForDeletion() {
        return OffsetDateTime.now(clock)
            .truncatedTo(ChronoUnit.DAYS)
            .minusDays(calculate(this.deletionDays));
    }

    public long calculate(long lastAccessedDeletionDays) {
        return lastAccessedDeletionDays + howManyOfPreviousDaysAreBankHolidaysOrWeekends(lastAccessedDeletionDays);
    }

    private long howManyOfPreviousDaysAreBankHolidaysOrWeekends(long lastAccessedDeletionDays) {
        long bankHolidayOrWeekendCount = 0;
        LocalDate today = LocalDate.now(clock);
        LocalDate cutOff = LocalDate.now(clock).minusDays(lastAccessedDeletionDays);
        List<Event> bankHolidays = bankHolidaysService.getBankHolidaysFor(OffsetDateTime.now().getYear());

        for (Event bankHoliday : bankHolidays) {
            if (isBankHolidayBetweenDates(bankHoliday, cutOff, today)) {
                bankHolidayOrWeekendCount++;
            }
        }
        bankHolidayOrWeekendCount += calculateWeekendDays(cutOff, today);
        return bankHolidayOrWeekendCount;
    }
}
