package uk.gov.hmcts.darts.common.service.bankholidays;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "bank-holidays-api", url = "${darts.bank-holidays.api.baseurl}")
@FunctionalInterface
public interface BankHolidaysApi {

    @GetMapping(path = "/bank-holidays.json")
    @Cacheable(value = "bank-holidays", key = " 'ENGLAND_WALES' ")
    BankHolidays retrieveAll();
}
