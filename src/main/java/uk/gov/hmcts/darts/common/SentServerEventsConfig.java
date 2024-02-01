package uk.gov.hmcts.darts.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class SentServerEventsConfig {

    @Value("${darts.sse.heartbeat: 5}")
    private long heartBeat;
    @Value("${darts.sse.sse-preview-timeout: 120}")
    private long previewTimeout;
}
