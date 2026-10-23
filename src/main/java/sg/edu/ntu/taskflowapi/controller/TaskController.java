package sg.edu.ntu.taskflowapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import jakarta.validation.Valid;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.service.TaskService;
import sg.edu.ntu.taskflowapi.dto.TaskMapper;
import sg.edu.ntu.taskflowapi.dto.TaskRequest;
import sg.edu.ntu.taskflowapi.dto.TaskResponse;
import sg.edu.ntu.taskflowapi.dto.StepMapper;
import sg.edu.ntu.taskflowapi.dto.StepRequest;
import sg.edu.ntu.taskflowapi.dto.StepResponse;

import java.util.List;
import java.time.LocalDate;

/**
 * Exposes task, search, summary, and nested-step REST operations.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    /**
     * @param taskService the selected task service implementation
     */
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** @return all tasks */
    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return TaskMapper.toTaskResponses(taskService.findAllTasks());
    }

    /**
     * @param id the task ID
     * @return the matching task
     */
    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return TaskMapper.toTaskResponse(taskService.findTaskById(id));
    }

    /**
     * @param request the validated task payload
     * @return the created task
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
        Task task = TaskMapper.toTask(request);
        return TaskMapper.toTaskResponse(taskService.createTask(task));
    }

    /**
     * @param id      the task ID
     * @param request the validated replacement values
     * @return the updated task
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        Task task = TaskMapper.toTask(request);
        return TaskMapper.toTaskResponse(taskService.updateTask(id, task));
    }

    /** @param id the task ID */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    /**
     * @param id the task ID
     * @return the completed task
     */
    @PutMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.OK)
    public TaskResponse markTaskAsCompleted(@PathVariable Long id) {
        return TaskMapper.toTaskResponse(taskService.markTaskAsCompleted(id));
    }

    /** @return a plain-text summary of the task list */
    @GetMapping(value = "/summary", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getTaskSummary() {
        return taskService.getTaskSummary();
    }

    /**
     * @param keyword text to find in titles or notes
     * @return matching tasks
     */
    @GetMapping("/search")
    public List<TaskResponse> searchTasksByKeyword(@RequestParam("keyword") String keyword) {
        return TaskMapper.toTaskResponses(taskService.searchTasksByKeyword(keyword));
    }

    /**
     * @param completed the required completion state
     * @return matching tasks
     */
    @GetMapping("/completed")
    public List<TaskResponse> getTasksByCompleted(@RequestParam("completed") boolean completed) {
        return TaskMapper.toTaskResponses(taskService.searchTasksByCompleted(completed));
    }

    /**
     * @param dueDate the exclusive due-date cutoff
     * @return pending tasks before the cutoff
     */
    @GetMapping("/due-before")
    public List<TaskResponse> getTasksDueBefore(@RequestParam @DateTimeFormat(iso = DATE) LocalDate dueDate) {
        return TaskMapper.toTaskResponses(taskService.searchTasksDueBefore(dueDate));
    }

    /**
     * @param taskId  the task ID
     * @param request the validated step payload
     * @return the created step
     */
    @PostMapping("/{taskId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public StepResponse addStepToTask(@PathVariable Long taskId, @Valid @RequestBody StepRequest request) {
        Step stepEntity = StepMapper.toStep(request);
        return StepMapper.toStepResponse(taskService.addStepToTask(taskId, stepEntity));
    }

    /**
     * @param taskId the task ID
     * @return the task's ordered steps
     */
    @GetMapping("/{taskId}/steps")
    public List<StepResponse> getStepsByTaskId(@PathVariable Long taskId) {
        return taskService.findStepsByTaskId(taskId).stream()
                .map(StepMapper::toStepResponse)
                .toList();
    }

    /**
     * @param taskId the task ID
     * @param stepId the step ID
     * @return the matching step
     */
    @GetMapping("/{taskId}/steps/{stepId}")
    public StepResponse getStepById(@PathVariable Long taskId, @PathVariable Long stepId) {

        return StepMapper.toStepResponse(taskService.findStepById(taskId, stepId));
    }

    /**
     * @param taskId the task ID
     * @param stepId the step ID
     * @param step   the validated replacement values
     * @return the updated step
     */
    @PutMapping("/{taskId}/steps/{stepId}")
    @ResponseStatus(HttpStatus.OK)
    public StepResponse updateStep(@PathVariable Long taskId, @PathVariable Long stepId,
            @Valid @RequestBody StepRequest step) {
        Step stepEntity = StepMapper.toStep(step);
        return StepMapper.toStepResponse(taskService.updateStep(taskId, stepId, stepEntity));
    }

    /**
     * @param taskId the task ID
     * @param stepId the step ID
     */
    @DeleteMapping("/{taskId}/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStep(@PathVariable Long taskId, @PathVariable Long stepId) {
        taskService.deleteStep(taskId, stepId);
    }

}
