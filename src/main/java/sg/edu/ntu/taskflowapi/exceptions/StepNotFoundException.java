package sg.edu.ntu.taskflowapi.exceptions;

/** Indicates that a requested step does not belong to the specified task. */
public class StepNotFoundException extends RuntimeException {

    /**
     * @param taskId the task ID
     * @param stepId the missing step ID
     */
    public StepNotFoundException(Long taskId, Long stepId) {
        super(String.format("Step %d was not found for task %d", stepId, taskId));
    }
}
