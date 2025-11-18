package uk.gov.hmcts.darts.util;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.task.runner.HasId;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.HasLongId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;


@SuppressWarnings("PMD.TooManyMethods")//Utility class with many small methods
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

    public static List<HearingEntity> orderHearingsByCreatedByAndId(Collection<HearingEntity> data) {
        return orderByCreatedByAndId(data, (o1, o2) -> {
            int compare = o1.getHearingDate().compareTo(o2.getHearingDate());
            if (compare == 0) {
                return compare(o1, o2);
            }
            return compare;
        });
    }

    public static <T extends CreatedBy & HasId<?>> List<T> orderByCreatedByAndId(Collection<T> data) {
        return orderByCreatedByAndId(data, DataUtil::compare);
    }

    public static <T extends CreatedBy & HasId<?>> List<T> orderByCreatedByAndId(Collection<T> data, Comparator<? super T> secondaryComparator) {
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
                        return secondaryComparator.compare(o1, o2);
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

    static int compare(Object id, Object id1) {
        if (id instanceof HasIntegerId && id1 instanceof HasIntegerId) {
            return Integer.compare(((HasIntegerId) id).getId(), ((HasIntegerId) id1).getId());
        } else if (id instanceof HasLongId && id1 instanceof HasLongId) {
            return Long.compare(((HasLongId) id).getId(), ((HasLongId) id1).getId());
        }
        throw new DartsApiException(CommonApiError.INTERNAL_SERVER_ERROR,
                                    "Cannot compare ids of type " + id.getClass().getName() + " and " + id1.getClass().getName());
    }

    public static boolean toBoolean(Boolean value) {
        return toBoolean(value, false);
    }

    public static boolean toBoolean(Boolean value, boolean defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    public static boolean isWithinBounds(Long value, Long min, Long max) {
        if (value == null) {
            return false;
        }
        if (min != null && value < min) {
            return false;
        }
        return max == null || value <= max;
    }

}
