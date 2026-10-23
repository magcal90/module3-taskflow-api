package sg.edu.ntu.taskflowapi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the Spring AI client used to generate task summaries. */
@Configuration
public class AiConfig {

    /**
     * @param builder the auto-configured chat client builder
     * @return the application chat client
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
