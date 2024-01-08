package uk.gov.hmcts.darts.audio.helper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.PreviewRange;

@Component
@RequiredArgsConstructor
public class BlobRangeHelper {

    @Value("${darts.audio.preview.max-preview-fetch-size}")
    int maxFetchSize;

    public PreviewRange getBlobRangeFromHeader(String httpRangeList) {
        if (StringUtils.isNotBlank(httpRangeList)) {
            httpRangeList = StringUtils.trim(httpRangeList);
            String rangeListValue = StringUtils.substringAfter(httpRangeList, "=");
            String[] ranges = rangeListValue.split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd;
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = rangeStart + maxFetchSize;
            }

            if (rangeEnd - rangeStart > maxFetchSize) {
                rangeEnd = rangeStart + maxFetchSize;
            }
            return new PreviewRange(rangeStart, rangeEnd, 0);
        } else {
            return new PreviewRange(0, maxFetchSize, 0);
        }
    }
}
