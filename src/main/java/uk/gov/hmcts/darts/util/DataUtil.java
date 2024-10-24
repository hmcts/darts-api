package uk.gov.hmcts.darts.util;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.util.Optional;

public final class DataUtil {
    private DataUtil() {

    }

    public static String toUpperCase(String value) {
        return Optional.ofNullable(value).map(String::toUpperCase).orElse(null);
    }

    public static void preProcess(DartsEvent dartsEvent) {
        dartsEvent.setCourthouse(DataUtil.toUpperCase(dartsEvent.getCourthouse()));
        dartsEvent.setCourtroom(DataUtil.toUpperCase(dartsEvent.getCourtroom()));
    }

    public static void preProcess(AddCaseRequest addCaseRequest) {
        addCaseRequest.setCourthouse(DataUtil.toUpperCase(addCaseRequest.getCourthouse()));
    }

    public static void preProcess(AddAudioMetadataRequest metadata) {
        metadata.setCourthouse(DataUtil.toUpperCase(metadata.getCourthouse()));
        metadata.setCourtroom(DataUtil.toUpperCase(metadata.getCourtroom()));
    }
}
