package sg.edu.ntu.taskflowapi.controller;

import org.springframework.format.annotation.DateTimeFormat;
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

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.service.TaskService;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    // Task APIs
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.findAllTasks();
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return taskService.findTaskById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@Valid @RequestBody Task task) {
        return taskService.createTask(task);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        return taskService.updateTask(id, task);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);

    }

    @PutMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.OK)
    public Task markTaskAsCompleted(@PathVariable Long id) {
        return taskService.markTaskAsCompleted(id);
    }

    @GetMapping(value = "/summary", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getTaskSummary() {
        return taskService.getTaskSummary();
    }

    // Task search APIs
    @GetMapping("/search")
    public List<Task> searchTasksByKeyword(@RequestParam("keyword") String keyword) {
        return taskService.searchTasksByKeyword(keyword);
    }

    @GetMapping("/completed")
    public List<Task> getTasksByCompleted(@RequestParam("completed") boolean completed) {
        return taskService.searchTasksByCompleted(completed);
    }

    @GetMapping("/due-before")
    public List<Task> getTasksDueBefore(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        return taskService.searchTasksDueBefore(dateTime);
    }

    // CRUD for steps within a task

    @PostMapping("/{taskId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public Step addStepToTask(@PathVariable Long taskId, @Valid @RequestBody Step step) {
        return taskService.addStepToTask(taskId, step);
    }

    @GetMapping("/{taskId}/steps")
    public List<Step> getStepsByTaskId(@PathVariable Long taskId) {
        return taskService.findStepsByTaskId(taskId);
    }

    @GetMapping("/{taskId}/steps/{stepId}")
    public Step getStepById(@PathVariable Long taskId, @PathVariable Long stepId) {

        return taskService.findStepById(taskId, stepId);
    }

    @PutMapping("/{taskId}/steps/{stepId}")
    @ResponseStatus(HttpStatus.OK)
    public Step updateStep(@PathVariable Long taskId, @PathVariable Long stepId, @Valid @RequestBody Step step) {

        return taskService.updateStep(taskId, stepId, step);
    }

    @DeleteMapping("/{taskId}/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStep(@PathVariable Long taskId, @PathVariable Long stepId) {
        taskService.deleteStep(taskId, stepId);
    }

}
