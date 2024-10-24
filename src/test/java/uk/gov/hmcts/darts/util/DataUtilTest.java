package uk.gov.hmcts.darts.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class DataUtilTest {

    @Test
    void toUpperCase() {
        assertThat(DataUtil.toUpperCase("test")).isEqualTo("TEST");
    }


    @Test
    void toUpperCaseNullValue() {
        assertThat(DataUtil.toUpperCase(null)).isNull();
    }

    @Test
    void preProcessDartsEvent() {
        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setCourthouse("courthouse");
        dartsEvent.setCourtroom("courtroom");
        DataUtil.preProcess(dartsEvent);
        assertThat(dartsEvent.getCourthouse()).isEqualTo("COURTHOUSE");
        assertThat(dartsEvent.getCourtroom()).isEqualTo("COURTROOM");
    }

    @Test
    void preProcessDartsEventWithNullValues() {
        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setCourthouse(null);
        dartsEvent.setCourtroom(null);
        DataUtil.preProcess(dartsEvent);
        assertThat(dartsEvent.getCourthouse()).isNull();
        assertThat(dartsEvent.getCourtroom()).isNull();
    }

    @Test
    void preProcessAddCaseRequest() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setCourthouse("courthouse");
        DataUtil.preProcess(addCaseRequest);
        assertThat(addCaseRequest.getCourthouse()).isEqualTo("COURTHOUSE");
    }

    @Test
    void preProcessAddCaseRequestWithNullValues() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setCourthouse(null);
        DataUtil.preProcess(addCaseRequest);
        assertThat(addCaseRequest.getCourthouse()).isNull();
    }

    @Test
    void preProcessAddAudioMetadataRequest() {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.setCourthouse("courthouse");
        addAudioMetadataRequest.setCourtroom("courtroom");
        DataUtil.preProcess(addAudioMetadataRequest);
        assertThat(addAudioMetadataRequest.getCourthouse()).isEqualTo("COURTHOUSE");
        assertThat(addAudioMetadataRequest.getCourtroom()).isEqualTo("COURTROOM");
    }

    @Test
    void preProcessAddAudioMetadataRequestWithNullValues() {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.setCourthouse(null);
        addAudioMetadataRequest.setCourtroom(null);
        DataUtil.preProcess(addAudioMetadataRequest);
        assertThat(addAudioMetadataRequest.getCourthouse()).isNull();
        assertThat(addAudioMetadataRequest.getCourtroom()).isNull();
    }
}
