package uk.gov.hmcts.darts.common.service.bankholidays;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class BankHolidaysService {

    private final BankHolidaysApi bankHolidaysApi;

    public List<Event> getBankHolidaysFor(int year) {
        return bankHolidaysApi.retrieveAll().englandAndWales.events.stream()
            .filter(eve -> eve.getDate().getYear() == year)
            .collect(toList());
    }
}
