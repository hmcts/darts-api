package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LastAccessedDeletionDayCalculator {

    private final BankHolidaysService bankHolidaysService;


    private final CurrentTimeHelper currentTimeHelper;

    private List<DayOfWeek> weekendDays = List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public OffsetDateTime getStartDateForDeletion(long numOfWorkingDaysToKeep) {
        List<LocalDate> bankHolidays = bankHolidaysService.getBankHolidaysLocalDateList();
        LocalDate deletionDate = getDeletionDate(bankHolidays, numOfWorkingDaysToKeep);
        return deletionDate.atStartOfDay().atOffset(ZoneOffset.UTC);
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

    private LocalDate getPreviousDay(LocalDate date, List<LocalDate> bankHolidays) {
        LocalDate newDate = date.minusDays(1);
        if (isBankHolidayOrWeekend(newDate, bankHolidays)) {
            //ignore and get previous day
            return getPreviousDay(newDate, bankHolidays);
        } else {
            return newDate;
        }
    }

    private LocalDate getDeletionDate(List<LocalDate> bankHolidays, long numOfDays) {
        LocalDate newDate = currentTimeHelper.currentLocalDate();
        for (int counter = 1; counter <= numOfDays; counter++) {
            newDate = getPreviousDay(newDate, bankHolidays);
        }
        return newDate;
    }
}
