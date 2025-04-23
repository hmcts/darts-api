package uk.gov.hmcts.darts.util;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;


public final class DataUtil {
    private DataUtil() {

    }

    public static String toUpperCase(String value) {
        return Optional.ofNullable(value).map(String::toUpperCase).orElse(null);
    }

    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public static List<String> trim(List<String> list) {
        if (isNull(list)) {
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
        dartsEvent.setCourthouse(toUpperCase(StringUtils.trimToEmpty(dartsEvent.getCourthouse())));
        dartsEvent.setCourtroom(toUpperCase(StringUtils.trimToEmpty(dartsEvent.getCourtroom())));
    }

    public static void preProcess(AddCaseRequest addCaseRequest) {
        addCaseRequest.setCourthouse(toUpperCase(StringUtils.trimToEmpty(addCaseRequest.getCourthouse())));
        addCaseRequest.defenders(trim(addCaseRequest.getDefenders()));
        addCaseRequest.prosecutors(trim(addCaseRequest.getProsecutors()));
        addCaseRequest.defendants(trim(addCaseRequest.getDefendants()));
    }

    public static void preProcess(AddAudioMetadataRequest metadata) {
        metadata.setCourthouse(toUpperCase(StringUtils.trimToEmpty(metadata.getCourthouse())));
        metadata.setCourtroom(toUpperCase(StringUtils.trimToEmpty(metadata.getCourtroom())));
    }

    public static <T extends CreatedBy & HasIntegerId> List<T> orderByCreatedByAndId(Collection<T> data) {
        List<T> sortedData = new ArrayList<>();
        if (CollectionUtils.isEmpty(data)) {
            return sortedData;
        }
        sortedData.addAll(
            data.stream()
                .filter(Objects::nonNull)
                .filter(entity -> entity.getId() != null)
                .filter(entity -> entity.getCreatedDateTime() != null)
                .sorted((o1, o2) -> {
                    int compare = o1.getCreatedDateTime().compareTo(o2.getCreatedDateTime());
                    if (compare == 0) {
                        return Integer.compare(o1.getId(), o2.getId());
                    }
                    return compare;
                })
                .toList());
        // Add entities without id or createdDateTime at the end
        sortedData.addAll(
            data.stream()
                .filter(Objects::nonNull)
                .filter(entity -> entity.getId() == null || entity.getCreatedDateTime() == null)
                .toList());
        return sortedData;
    }
}
