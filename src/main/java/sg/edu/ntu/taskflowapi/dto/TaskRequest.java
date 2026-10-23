package sg.edu.ntu.taskflowapi.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

public record TaskRequest(@NotBlank(message = "Title is mandatory") String title,
                        String note,
                        boolean completed,
                        @FutureOrPresent LocalDate dueDate) {

}

