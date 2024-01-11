package uk.gov.hmcts.darts.common.service.bankholidays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BankHolidaysServiceTest {

    @Mock
    private BankHolidaysApi bankHolidaysApi;
    private BankHolidaysService bankHolidaysService;

    @BeforeEach
    void setUp() {
        bankHolidaysService = new BankHolidaysService(bankHolidaysApi);
    }

    @ParameterizedTest
    @CsvSource({"2018-08-23,", ",2018-08-23", ","})
    void returnsEmptyListWhenAnyParamIsNull(LocalDate startDate, LocalDate endDate) {
        var bankHolidays = bankHolidaysService.getBankHolidaysAfterStartDateAndBeforeEndDate(startDate, endDate);
        assertEquals(0, bankHolidays.size());
    }
}
