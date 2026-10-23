package sg.edu.ntu.taskflowapi.dto;

import java.time.LocalDate;
import java.util.List;

public record TaskResponse(Long id,
        String title,
        String note,
        boolean completed,
        LocalDate dueDate,
        List<StepResponse> steps) {
    
}
