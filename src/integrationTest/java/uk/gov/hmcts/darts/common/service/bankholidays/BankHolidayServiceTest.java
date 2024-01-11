package uk.gov.hmcts.darts.common.service.bankholidays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.BankHolidayApiStub;

import java.io.IOException;
import java.time.LocalDate;

import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.Assertions.assertThat;


class BankHolidayServiceTest extends IntegrationBase {

    @Autowired
    private BankHolidaysService bankHolidaysService;

    @Value("classpath:tests/bank-holidays/test-data.json")
    private Resource bankHolidayData2018To2026;

    private final BankHolidayApiStub bankHolidayApiStub = new BankHolidayApiStub();

    @BeforeEach
    void setUp() throws IOException {
        bankHolidayApiStub.returns(bankHolidayData2018To2026.getContentAsString(defaultCharset()));
    }

    @Test
    void returnsBankHolidaysWithinDateRange() {
        var startDate = LocalDate.of(2023, 12, 25);
        var endDate = LocalDate.of(2024, 1, 10);

        var bankHolidays = bankHolidaysService.getBankHolidaysAfterStartDateAndBeforeEndDate(startDate, endDate);

        assertThat(bankHolidays.size()).isEqualTo(2);
    }

    @Test
    void returnsBankHolidays() {
        var bankHolidays = bankHolidaysService.getBankHolidays(2020);

        assertThat(bankHolidays.size()).isEqualTo(8);
    }

    @Test
    void returnEmptyListForNotFoundYear() {
        var bankHolidays1 = bankHolidaysService.getBankHolidays(1980);

        assertThat(bankHolidays1.size()).isEqualTo(0);
    }
}
