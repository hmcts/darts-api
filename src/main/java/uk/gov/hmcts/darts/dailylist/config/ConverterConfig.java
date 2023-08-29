package uk.gov.hmcts.darts.dailylist.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.darts.dailylist.converter.DailyListJsonConverter;

@Configuration
@RequiredArgsConstructor
public class ConverterConfig implements WebMvcConfigurer {
    private final DailyListJsonConverter dailyListJsonConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(dailyListJsonConverter);
    }
}
