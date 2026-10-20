package sg.edu.ntu.taskflowapi.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@JsonPropertyOrder({ "id", "title", "description", "completed", "dueAt" })
public class Task {
    private Long id;
    private String title;
    private String description;
    private boolean completed;
    private LocalDateTime dueAt;

}
