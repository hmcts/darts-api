package uk.gov.hmcts.darts.util;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.util.List;
import java.util.Optional;

public final class DataUtil {
    private DataUtil() {
        // Utility class
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
        dartsEvent.setCourthouse(toUpperCase(dartsEvent.getCourthouse()));
        dartsEvent.setCourtroom(toUpperCase(dartsEvent.getCourtroom()));
    }

    public static void preProcess(AddCaseRequest addCaseRequest) {
        addCaseRequest.setCourthouse(toUpperCase(addCaseRequest.getCourthouse()));
        addCaseRequest.defenders(trim(addCaseRequest.getDefenders()));
        addCaseRequest.prosecutors(trim(addCaseRequest.getProsecutors()));
        addCaseRequest.defendants(trim(addCaseRequest.getDefendants()));
    }

    public static void preProcess(AddAudioMetadataRequest metadata) {
        metadata.setCourthouse(toUpperCase(metadata.getCourthouse()));
        metadata.setCourtroom(toUpperCase(metadata.getCourtroom()));
    }
}
