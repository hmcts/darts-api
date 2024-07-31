package uk.gov.hmcts.darts.common.config.mode;


import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ConditionOnNotAts implements Condition {
    private final ConditionOnAts conditionOnAts = new ConditionOnAts();

    public ConditionOnNotAts() {
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !conditionOnAts.matches(context, metadata);
    }
}