package uk.gov.hmcts.darts.reports.service;

import uk.gov.hmcts.darts.reports.model.AdminReportsResponseItem;

@FunctionalInterface
public interface ReportsAdminService {

    AdminReportsResponseItem getAdminReports();

}
