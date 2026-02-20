package uk.gov.hmcts.darts.retention.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class RetentionConfidenceCategoryEnumConverter implements AttributeConverter<RetentionConfidenceCategoryEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RetentionConfidenceCategoryEnum retentionConfidenceCategoryEnum) {
        if (retentionConfidenceCategoryEnum == null) {
            return null;
        }
        return retentionConfidenceCategoryEnum.getId();
    }

    @Override
    public RetentionConfidenceCategoryEnum convertToEntityAttribute(Integer id) {
        if (id == null) {
            return null;
        }

        return Stream.of(RetentionConfidenceCategoryEnum.values())
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(RetentionConfidenceCategoryEnum.UNKNOWN);
    }
}