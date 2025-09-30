package uk.gov.hmcts.darts.reports.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ReportsAdminServiceImplTest {

    @Test
    void getAdminReports() {
        ReportsAdminServiceImpl reportsAdminService = new ReportsAdminServiceImpl();
        assertNull(reportsAdminService.getAdminReports());
    }
}