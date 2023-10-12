package uk.gov.hmcts.darts.common.service.bankholidays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.BankHolidayApiStub;

import static org.assertj.core.api.Assertions.assertThat;

//@ActiveProfiles("in-memory-caching")
class BankHolidayServiceTest extends IntegrationBase {

    public static final String VALID_BANK_HOLIDAY_JSON = """
        {
          "england-and-wales": {
            "division": "england-and-wales",
            "events": [
              {
                "title": "New Year’s Day",
                "date": "2020-01-01",
                "notes": "",
                "bunting": true
              },
              {
                "title": "Good Friday",
                "date": "2018-03-30",
                "notes": "",
                "bunting": false
              }
            ]
          },
          "some-other-division": {
            "division": "some-other-division",
            "events": [
              {
                "title": "New Year’s Day",
                "date": "2018-01-01",
                "notes": "",
                "bunting": true
              }
            ]
          }
        }
        """;

    @Autowired
    private BankHolidaysService bankHolidaysService;

    private final BankHolidayApiStub bankHolidayApiStub = new BankHolidayApiStub();

    @Test
    void returnsBankHolidays() {
        bankHolidayApiStub.returns(VALID_BANK_HOLIDAY_JSON);

        var bankHolidays1 = bankHolidaysService.getBankHolidaysFor(2020);

        assertThat(bankHolidays1.size()).isEqualTo(1);




    }

}
