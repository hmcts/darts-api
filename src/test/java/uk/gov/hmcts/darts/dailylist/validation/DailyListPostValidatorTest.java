package uk.gov.hmcts.darts.dailylist.validation;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyListPostValidatorTest {

    @Test
    void ok_justJson() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListJson(new DailyListJsonObject());
        DailyListPostValidator.validate(request);
    }

    @Test
    void ok_Xml() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setMessageId("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        DailyListPostValidator.validate(request);
    }

    @Test
    void ok_XmlAndJson() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        request.setDailyListJson(new DailyListJsonObject());
        DailyListPostValidator.validate(request);
    }

    @Test
    void error_Xml_missingUniqueId() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingXml() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("Either xml_document");
    }

    @Test
    void error_Xml_missingSourceSystem() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingCourthouse() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingHearingDate() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setMessageId("test");
        request.setPublishedDateTime(OffsetDateTime.now());
        request.setMessageId("test");
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingPublishedDateTime() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setMessageId("test");
        request.setHearingDate(LocalDate.now());
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void error_Xml_missingMessageId() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("CPP");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setPublishedDateTime(OffsetDateTime.now());
        Exception exception = assertThrows(DartsApiException.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("If xml_document is being provided");
    }

    @Test
    void shouldThrowExceptionWhenJsonAndSourceSystemInvalid() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setSourceSystem("RUB");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setDailyListJson(new DailyListJsonObject());
        Exception exception = assertThrows(Exception.class, () -> DailyListPostValidator.validate(request));
        assertThat(exception.getMessage()).contains("Invalid source system. Should be CPP or XHB.");
    }

    @Test
    void shouldSuccessfullyValidateWhenJsonAndNoSourceSystem() {
        DailyListPostRequestInternal request = new DailyListPostRequestInternal();
        request.setDailyListXml("test");
        request.setUniqueId("test");
        request.setCourthouse("test");
        request.setHearingDate(LocalDate.now());
        request.setDailyListJson(new DailyListJsonObject());
        DailyListPostValidator.validate(request);
    }

}
