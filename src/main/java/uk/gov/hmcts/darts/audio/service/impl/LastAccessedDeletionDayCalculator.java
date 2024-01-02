package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LastAccessedDeletionDayCalculator {

    private final BankHolidaysService bankHolidaysService;


    private final CurrentTimeHelper currentTimeHelper;

    private List<DayOfWeek> weekendDays = List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public OffsetDateTime getStartDateForDeletion(long numOfWorkingDaysToKeep) {
        List<LocalDate> bankHolidays = bankHolidaysService.getBankHolidaysLocalDateList();
        OffsetDateTime deletionDate = subtractNumOfDaysFromDate(bankHolidays, numOfWorkingDaysToKeep);
        return deletionDate;
    }

    private boolean isBankHoliday(LocalDate date, List<LocalDate> bankHolidays) {
        return bankHolidays.contains(date);
    }

    private boolean isWeekend(LocalDate date) {
        return weekendDays.contains(date.getDayOfWeek());
    }

    private boolean isBankHolidayOrWeekend(LocalDate date, List<LocalDate> bankHolidays) {
        return isWeekend(date) || isBankHoliday(date, bankHolidays);
    }

    private OffsetDateTime getPreviousDay(OffsetDateTime date, List<LocalDate> bankHolidays) {
        OffsetDateTime newDate = date.minusDays(1);
        if (isBankHolidayOrWeekend(newDate.toLocalDate(), bankHolidays)) {
            //ignore and get previous day
            return getPreviousDay(newDate, bankHolidays);
        } else {
            return newDate;
        }
    }

    private OffsetDateTime subtractNumOfDaysFromDate(List<LocalDate> bankHolidays, long numOfDays) {
        OffsetDateTime newDate = currentTimeHelper.currentOffsetDateTime();
        for (int counter = 1; counter <= numOfDays; counter++) {
            newDate = getPreviousDay(newDate, bankHolidays);
        }
        return newDate;
    }
}
