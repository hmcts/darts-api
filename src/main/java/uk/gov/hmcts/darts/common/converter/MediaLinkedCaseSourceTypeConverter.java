package uk.gov.hmcts.darts.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class MediaLinkedCaseSourceTypeConverter implements AttributeConverter<MediaLinkedCaseSourceType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MediaLinkedCaseSourceType sourceType) {
        if (sourceType == null) {
            return null;
        }
        return sourceType.getId();
    }

    @Override
    public MediaLinkedCaseSourceType convertToEntityAttribute(Integer id) {
        if (id == null) {
            return null;
        }

        return Stream.of(MediaLinkedCaseSourceType.values())
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}
