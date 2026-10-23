package sg.edu.ntu.taskflowapi.dto;

import jakarta.validation.constraints.NotBlank;

public record StepRequest(@NotBlank(message = "Description is mandatory") String description) {

}
