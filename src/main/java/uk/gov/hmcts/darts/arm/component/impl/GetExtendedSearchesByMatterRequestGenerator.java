package uk.gov.hmcts.darts.arm.component.impl;

import lombok.Builder;
import lombok.NonNull;

import static uk.gov.hmcts.darts.arm.util.ArmRpoJsonUtil.sanitise;

@Builder
public class GetExtendedSearchesByMatterRequestGenerator {

    @NonNull
    private String matterId;

    private static final String REQUEST_TEMPLATE = """
        {
          "filter": "1",
          "filterBy": {},
          "matterId": "%s",
          "usePaging": true,
          "rowsNumber": 10,
          "pageIndex": 0,
          "orderBy": "createdDate",
          "orderByAsc": false,
          "search": ""
        } 
        """;

    public String getJsonRequest() {
        String templatedJsonRequest = REQUEST_TEMPLATE.formatted(matterId);
        return sanitise(templatedJsonRequest);
    }
}
