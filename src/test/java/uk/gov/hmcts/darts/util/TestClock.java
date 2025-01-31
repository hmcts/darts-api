package uk.gov.hmcts.darts.util;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;

public class TestClock extends Clock {

    private OffsetDateTime offsetDateTime;

    public TestClock() {
        this(OffsetDateTime.now());
    }

    public TestClock(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public void updateClock(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public void addTime(long amountToAdd, TemporalUnit unit) {
        this.updateClock(this.offsetDateTime.plus(amountToAdd, unit));
    }

    public void subtractTime(long amountToAdd, TemporalUnit unit) {
        this.updateClock(this.offsetDateTime.minus(amountToAdd, unit));
    }

    @Override
    public ZoneId getZone() {
        return offsetDateTime.toZonedDateTime().getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(offsetDateTime.atZoneSameInstant(zone).toOffsetDateTime());
    }

    @Override
    public Instant instant() {
        return offsetDateTime.toInstant();
    }
}
