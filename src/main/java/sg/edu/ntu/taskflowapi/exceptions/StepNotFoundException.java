package sg.edu.ntu.taskflowapi.exceptions;

public class StepNotFoundException extends RuntimeException {

    public StepNotFoundException(Long taskId, Long stepId) {
        super(String.format("Step %d was not found for task %d", stepId, taskId));
    }
}
