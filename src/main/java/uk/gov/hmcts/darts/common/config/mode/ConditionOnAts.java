package uk.gov.hmcts.darts.common.config.mode;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import uk.gov.hmcts.darts.DartsMode;

public class ConditionOnAts implements Condition {
    public ConditionOnAts() {
    }

    @Override
    public boolean matches(ConditionContext context,
                           AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return env.getProperty(DartsMode.ATS_MODE.getModeStr()) != null;
    }
}