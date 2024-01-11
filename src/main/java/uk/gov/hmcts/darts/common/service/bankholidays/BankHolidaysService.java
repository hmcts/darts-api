package uk.gov.hmcts.darts.common.service.bankholidays;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class BankHolidaysService {

    private final BankHolidaysApi bankHolidaysApi;

    public List<Event> getBankHolidays(int year) {
        return bankHolidaysApi.retrieveAll().englandAndWales.events.stream()
            .filter(eve -> eve.getDate().getYear() == year).toList();
    }

    public List<Event> getBankHolidays() {
        return new ArrayList<>(bankHolidaysApi.retrieveAll().englandAndWales.events);
    }

    public List<LocalDate> getBankHolidaysLocalDateList() {
        return getBankHolidays().stream().map(Event::getDate).toList();
    }

    public List<LocalDate> getBankHolidaysAfterStartDateAndBeforeEndDate(LocalDate startDate, LocalDate endDate) {
        if (isNull(startDate) || isNull(endDate)) {
            return emptyList();
        }

        return getBankHolidays().stream()
            .map(Event::getDate)
            .filter(date -> date.isAfter(startDate) && date.isBefore(endDate))
            .toList();
    }
}
