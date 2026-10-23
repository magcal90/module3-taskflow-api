package sg.edu.ntu.taskflowapi.config;

import java.time.Clock;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(TimeProperties properties) {
        return Clock.system(properties.timeZone());
    }

    @Bean
    public ValidationConfigurationCustomizer validationClockProvider(Clock clock) {
        return configuration ->
                configuration.clockProvider(() -> clock);
    }
}
