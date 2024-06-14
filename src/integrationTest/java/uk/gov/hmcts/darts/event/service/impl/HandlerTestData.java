package uk.gov.hmcts.darts.event.service.impl;

import uk.gov.hmcts.darts.testutils.IntegrationBaseWithGatewayStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.time.OffsetDateTime.now;

class HandlerTestData extends IntegrationBaseWithGatewayStub {

    protected static final String SOME_COURTHOUSE = "some-courthouse";
    protected static final String UNKNOWN_COURTHOUSE = "unknown-courthouse";
    protected static final String SOME_ROOM = "some-room";
    protected static final String SOME_OTHER_ROOM = "some-other-room";

    protected static final String SOME_CASE_NUMBER = "CASE1";
    protected static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 23, 10, 0, 0);
    protected static final OffsetDateTime HEARING_DATE_ODT = HEARING_DATE.atOffset(ZoneOffset.UTC);

    protected final OffsetDateTime today = now();
}
