package uk.gov.hmcts.darts.dailylist.validation;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyListPostValidatorTest {

    @Test
    void ok_justJson() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListJson(new DailyListJsonObject());
        DailyListPostValidator.validate(request);
    }

    @Test
    void ok_Xml() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        DailyListPostValidator.validate(request);
    }

    @Test
    void ok_XmlAndJson() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setDailyListJson(new DailyListJsonObject());
        DailyListPostValidator.validate(request);
    }

    @Test
    void error_Xml_missingUniqueId() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setSourceSystem("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());

        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingXml() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setUniqueId("test");
        request.setSourceSystem("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("Either xml_document");
    }

    @Test
    void error_Xml_missingSourceSystem() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingCourthouse() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingHearingDate() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("test");
        request.setCourthouse("test");
        request.setPublishedDateTime(OffsetDateTime.now());
        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingPublishedDateTime() {
        DailyListPostRequest request = new DailyListPostRequest();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

}
