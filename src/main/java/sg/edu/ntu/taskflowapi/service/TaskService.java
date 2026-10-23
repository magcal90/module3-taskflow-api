package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.exceptions.StepNotFoundException;
import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;

import java.util.List;
import java.time.LocalDate;

/**
 * Defines task management, search, summary, and nested-step operations.
 */
public interface TaskService {

    /** @return all tasks */
    List<Task> findAllTasks();

    /**
     * Finds a task by ID.
     *
     * @param id the task ID
     * @return the matching task
     * @throws TaskNotFoundException if the task does not exist
     */
    Task findTaskById(Long id);

    /**
     * Creates a task.
     *
     * @param task the task to create
     * @return the created task, including its assigned ID
     */
    Task createTask(Task task);

    /**
     * Replaces the editable fields of a task.
     *
     * @param id the task ID
     * @param task the replacement values
     * @return the updated task
     * @throws TaskNotFoundException if the task does not exist
     */
    Task updateTask(Long id, Task task);

    /**
     * Deletes a task.
     *
     * @param id the task ID
     * @throws TaskNotFoundException if the task does not exist
     */
    void deleteTask(Long id);

    /**
     * Marks a task as completed.
     *
     * @param id the task ID
     * @return the completed task
     * @throws TaskNotFoundException if the task does not exist
     */
    Task markTaskAsCompleted(Long id);

    /**
     * Generates a plain-English summary of all tasks.
     *
     * @return a fixed empty-list message or an AI-generated summary
     */
    String getTaskSummary();

    /**
     * Searches task titles and notes without regard to case.
     *
     * @param keyword the text to find
     * @return matching tasks
     */
    List<Task> searchTasksByKeyword(String keyword);

    /**
     * Filters tasks by completion state.
     *
     * @param completed the required completion state
     * @return matching tasks
     */
    List<Task> searchTasksByCompleted(boolean completed);

    /**
     * Finds incomplete tasks due strictly before a date, ordered by due date.
     *
     * @param dueDate the exclusive cutoff date
     * @return matching tasks in ascending due-date order
     */
    List<Task> searchTasksDueBefore(LocalDate dueDate);

    /**
     * Adds the next ordered step to a task.
     *
     * @param id the task ID
     * @param step the step to add
     * @return the created step
     * @throws TaskNotFoundException if the task does not exist
     */
    Step addStepToTask(Long id, Step step);

    /**
     * Lists a task's steps in ascending order.
     *
     * @param taskId the task ID
     * @return the ordered steps
     * @throws TaskNotFoundException if the task does not exist
     */
    List<Step> findStepsByTaskId(Long taskId);

    /**
     * Finds a step belonging to a task.
     *
     * @param taskId the task ID
     * @param stepId the step ID
     * @return the matching step
     * @throws TaskNotFoundException if the task does not exist
     * @throws StepNotFoundException if the step does not exist for the task
     */
    Step findStepById(Long taskId, Long stepId);

    /**
     * Updates a step's description.
     *
     * @param taskId the task ID
     * @param stepId the step ID
     * @param step the replacement values
     * @return the updated step
     * @throws TaskNotFoundException if the task does not exist
     * @throws StepNotFoundException if the step does not exist for the task
     */
    Step updateStep(Long taskId, Long stepId, Step step);

    /**
     * Deletes a step belonging to a task.
     *
     * @param taskId the task ID
     * @param stepId the step ID
     * @throws TaskNotFoundException if the task does not exist
     * @throws StepNotFoundException if the step does not exist for the task
     */
    void deleteStep(Long taskId, Long stepId);
    
}
