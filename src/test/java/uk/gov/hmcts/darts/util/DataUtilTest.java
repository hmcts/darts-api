package uk.gov.hmcts.darts.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.util.ArrayList;
import java.util.List;

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
    void trimList() {
        assertThat(DataUtil.trim(
            List.of(" test ", "  test2  ", "test3  ", "   test4", "   test 5 with more spaces  ")))
            .containsExactly("test", "test2", "test3", "test4", "test 5 with more spaces");
    }

    @Test
    void trimListNullList() {
        assertThat(DataUtil.trim((List<String>) null)).isNull();
    }

    @Test
    void trimListWithNullListValue() {
        List<String> list = new ArrayList<>(List.of(" test ", "  test2  ", "test3  ", "   test4", "   test 5 with more spaces  "));
        list.add(null);
        assertThat(DataUtil.trim(list))
            .containsExactly("test", "test2", "test3", "test4", "test 5 with more spaces", null);
    }

    @Test
    void trim() {
        assertThat(DataUtil.trim(" test ")).isEqualTo("test");
    }

    @Test
    void trimNull() {
        assertThat(DataUtil.trim((String) null)).isNull();
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
        assertThat(dartsEvent.getCourthouse()).isEmpty();
        assertThat(dartsEvent.getCourtroom()).isEmpty();
    }

    @Test
    void preProcessAddCaseRequest() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setCourthouse("courthouse");
        addCaseRequest.setDefenders(List.of("  defender1   ", "   defender2"));
        addCaseRequest.setProsecutors(List.of("  prosecutor1   ", "   prosecutor2"));
        addCaseRequest.setDefendants(List.of("  defendant1   ", "   defendant2"));
        DataUtil.preProcess(addCaseRequest);
        assertThat(addCaseRequest.getCourthouse()).isEqualTo("COURTHOUSE");
        assertThat(addCaseRequest.getDefenders()).containsExactly("defender1", "defender2");
        assertThat(addCaseRequest.getProsecutors()).containsExactly("prosecutor1", "prosecutor2");
        assertThat(addCaseRequest.getDefendants()).containsExactly("defendant1", "defendant2");
    }

    @Test
    void preProcessAddCaseRequestWithNullValues() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setCourthouse(null);
        DataUtil.preProcess(addCaseRequest);
        assertThat(addCaseRequest.getCourthouse()).isEmpty();
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
        assertThat(addAudioMetadataRequest.getCourthouse()).isEmpty();
        assertThat(addAudioMetadataRequest.getCourtroom()).isEmpty();
    }
}
