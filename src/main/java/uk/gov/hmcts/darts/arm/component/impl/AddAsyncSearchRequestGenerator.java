package uk.gov.hmcts.darts.arm.component.impl;

import lombok.Builder;
import lombok.NonNull;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.darts.arm.util.ArmRpoJsonUtil.sanitise;

@Builder
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class AddAsyncSearchRequestGenerator {

    @NonNull
    private String name;

    @NonNull
    private String searchName;

    @NonNull
    private String matterId;

    @NonNull
    private String entitlementId;

    @NonNull
    private String indexId;

    @NonNull
    private String sortingField;

    @NonNull
    private OffsetDateTime startTime;

    @NonNull
    private OffsetDateTime endTime;

    private static final String REQUEST_TEMPLATE = """
        {
          "queryTree": {
            "operator": 1,
            "children": [
              {
                "field": {
                  "dataScopeEntitlementNotApplied": true,
                  "removable": false,
                  "draggable": false,
                  "fixedField": true,
                  "history": false,
                  "error": false,
                  "name": "record_class",
                  "valueType": 32,
                  "value": [
                    "DARTS"
                  ]
                }
              },
              {
                "field": {
                  "dataScopeEntitlementNotApplied": false,
                  "removable": true,
                  "draggable": true,
                  "fixedField": false,
                  "history": false,
                  "error": false,
                  "valueType": 7,
                  "name": "ingestionDate",
                  "value": [
                    "%s",
                    "%s"
                  ]
                }
              }
            ]
          },
          "queryTreeWithin": {
            "operator": 1,
            "children": [
              {
                "field": {
                  "dataScopeEntitlementNotApplied": false,
                  "removable": true,
                  "draggable": true,
                  "fixedField": false,
                  "history": false,
                  "error": false,
                  "valueType": -1,
                  "name": "",
                  "value": []
                }
              }
            ]
          },
          "queryFields": {},
          "clientTimezone": "Europe/London",
          "timezone": "Europe/London",
          "matterId": "%s",
          "entitlementId": "%s",
          "indexId": "%s",
          "sortingField": "%s",
          "sortingType": 1,
          "name": "%s",
          "searchName": "%s"
        }
        """;

    public String getJsonRequest() {
        String templatedJsonRequest = REQUEST_TEMPLATE.formatted(startTime.format(DateTimeFormatter.ISO_DATE_TIME),
                                                                 endTime.format(DateTimeFormatter.ISO_DATE_TIME),
                                                                 matterId,
                                                                 entitlementId,
                                                                 indexId,
                                                                 sortingField,
                                                                 name,
                                                                 searchName);
        return sanitise(templatedJsonRequest);
    }


}
