package uk.gov.hmcts.darts.notification.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;

@Configuration
public class BeanConfiguration {

    @Value("${darts.notification.gov-notify.api-key}")
    private String apiKey;

    @Bean
    public NotificationClient notificationClient() {
        return new NotificationClient(apiKey);
    }



}
