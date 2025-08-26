package uk.gov.hmcts.darts.event.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@ConfigurationProperties("darts.events")
@Getter
@Setter
@Configuration
public class EventConfig {

    List<String> standardEventTypesWithRetention;

}

