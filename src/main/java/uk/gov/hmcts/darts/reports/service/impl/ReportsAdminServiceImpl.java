package uk.gov.hmcts.darts.reports.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.reports.model.AdminReportsResponseItem;
import uk.gov.hmcts.darts.reports.service.ReportsAdminService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsAdminServiceImpl implements ReportsAdminService {

    @Override
    public AdminReportsResponseItem getAdminReports() {
        return null;
    }

}
