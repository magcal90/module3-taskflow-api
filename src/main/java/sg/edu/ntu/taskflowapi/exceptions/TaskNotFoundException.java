package sg.edu.ntu.taskflowapi.exceptions;

/** Indicates that a requested task does not exist. */
public class TaskNotFoundException extends RuntimeException {

    /**
     * @param id the missing task ID
     */
    public TaskNotFoundException(Long id) {
        super("Task not found: " + id);
    }
}
