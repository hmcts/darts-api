package uk.gov.hmcts.darts.util;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.util.List;
import java.util.Optional;

public final class DataUtil {
    private DataUtil() {

    }

    public static String toUpperCase(String value) {
        return Optional.ofNullable(value).map(String::toUpperCase).orElse(null);
    }

    public static List<String> trim(List<String> list) {
        if (list == null) {
            return null;
        }
        return list.stream()
            .map(DataUtil::trim)
            .toList();
    }

    public static String trim(String item) {
        return Optional.ofNullable(item)
            .map(String::trim)
            .orElse(null);
    }

    public static void preProcess(DartsEvent dartsEvent) {
        dartsEvent.setCourthouse(DataUtil.toUpperCase(dartsEvent.getCourthouse()));
        dartsEvent.setCourtroom(DataUtil.toUpperCase(dartsEvent.getCourtroom()));
    }

    public static void preProcess(AddCaseRequest addCaseRequest) {
        addCaseRequest.setCourthouse(DataUtil.toUpperCase(addCaseRequest.getCourthouse()));
        addCaseRequest.defenders(DataUtil.trim(addCaseRequest.getDefenders()));
        addCaseRequest.prosecutors(DataUtil.trim(addCaseRequest.getProsecutors()));
        addCaseRequest.defendants(DataUtil.trim(addCaseRequest.getDefendants()));
    }

    public static void preProcess(AddAudioMetadataRequest metadata) {
        metadata.setCourthouse(DataUtil.toUpperCase(metadata.getCourthouse()));
        metadata.setCourtroom(DataUtil.toUpperCase(metadata.getCourtroom()));
    }
}
