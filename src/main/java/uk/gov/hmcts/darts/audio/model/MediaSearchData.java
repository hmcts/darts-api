package uk.gov.hmcts.darts.audio.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class MediaSearchData {
    final Integer transformedMediaId;

    final List<Integer> hearingIds;

    final OffsetDateTime startDateTime;

    final OffsetDateTime endDateTime;
}