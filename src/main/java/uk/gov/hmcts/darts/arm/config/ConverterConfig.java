package uk.gov.hmcts.darts.arm.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.darts.arm.converter.UploadNewFileRecordConverter;

@Configuration
@RequiredArgsConstructor
public class ConverterConfig implements WebMvcConfigurer {
    private final UploadNewFileRecordConverter uploadNewFileRecordConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(uploadNewFileRecordConverter);
    }
}
