package uk.gov.hmcts.darts.retention.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class RetentionConfidenceScoreEnumConverter implements AttributeConverter<RetentionConfidenceScoreEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RetentionConfidenceScoreEnum retentionConfidenceScoreEnum) {
        if (retentionConfidenceScoreEnum == null) {
            return null;
        }
        return retentionConfidenceScoreEnum.getId();
    }

    @Override
    public RetentionConfidenceScoreEnum convertToEntityAttribute(Integer id) {
        if (id == null) {
            return null;
        }

        return Stream.of(RetentionConfidenceScoreEnum.values())
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}