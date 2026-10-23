package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.repository.StepRepository;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;
import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;
import sg.edu.ntu.taskflowapi.exceptions.StepNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/** JPA-backed task service selected by the {@code jpa} profile. 
 *  @Transactional - Keeps the entity fetch and subsequent mutation atomic within one transaction.
 * 
*/
@Service
@Profile("jpa")
@Primary
@Slf4j

public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final StepRepository stepRepository;
    private final ChatClient chatClient;
    private static final boolean COMPLETED = true;

    public TaskServiceImpl(TaskRepository taskRepository, StepRepository stepRepository, ChatClient chatClient) {
        this.taskRepository = taskRepository;
        this.stepRepository = stepRepository;
        this.chatClient = chatClient;
    }

    @Override
    public List<Task> findAllTasks() {

        return taskRepository.findAll();
    }

    @Override
    public Task findTaskById(Long id) {

        return findTaskByIdOrThrow(id);
    }

    @Override
    @Transactional
    public Task createTask(Task task) {
        Task savedTask = taskRepository.save(task);
        log.info("Task created: taskId={}, title={}", savedTask.getId(), savedTask.getTitle());

        return savedTask;
    }

    @Override
    @Transactional
    public Task updateTask(Long id, Task task) {
        Task existingTask = findTaskByIdOrThrow(id);
        existingTask.setTitle(task.getTitle());
        existingTask.setNote(task.getNote());
        existingTask.setCompleted(task.isCompleted());
        existingTask.setDueDate(task.getDueDate());
        Task savedTask = taskRepository.save(existingTask);
        log.info("Task updated: taskId={}", id);

        return savedTask;
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskByIdOrThrow(id);
        taskRepository.deleteById(task.getId());
        log.info("Task deleted: taskId={}", id);
    }

    @Override
    @Transactional
    public Task markTaskAsCompleted(Long id) {
        Task task = findTaskByIdOrThrow(id);
        task.setCompleted(COMPLETED);
        Task savedTask = taskRepository.save(task);
        log.info("Task completed: taskId={}", id);

        return savedTask;
    }

    @Override
    public String getTaskSummary() {

        List<Task> tasks = taskRepository.findAll();

        if (tasks.isEmpty()) {
            return "There are no tasks.";
        }

        final LocalDate today = LocalDate.now();

        String taskList = tasks.stream()
                .map(task -> TaskSummaryHelper.formatTasks(task, today))
                .collect(Collectors.joining("\n"));

        log.info("Generating task summary for {} tasks", tasks.size());

        return chatClient
                .prompt()
                .system(TaskSummaryHelper.SYSTEM_PROMPT)
                .user(TaskSummaryHelper.formatUserPrompt(taskList))
                .call()
                .content();

    }

    @Override
    public List<Task> searchTasksByKeyword(String keyword) {
        List<Task> tasks = taskRepository.findByTitleContainingIgnoreCaseOrNoteContainingIgnoreCase(keyword, keyword);
        log.debug("Task keyword search completed: keyword={}, resultCount={}", keyword, tasks.size());

        return tasks;
    }

    @Override
    public List<Task> searchTasksByCompleted(boolean completed) {

        return taskRepository.findByCompleted(completed);
    }

    @Override
    public List<Task> searchTasksDueBefore(LocalDate dueDate) {

        return taskRepository.findByCompletedFalseAndDueDateBeforeOrderByDueDateAsc(dueDate);
    }

    @Override
    @Transactional
    public Step addStepToTask(Long taskId, Step step) {
        Task task = findTaskByIdOrThrow(taskId);

        int nextStepOrder = stepRepository
                .findTopByTaskIdOrderByStepOrderDesc(taskId)
                .map(lastStep -> lastStep.getStepOrder() + 1)
                .orElse(1);

        step.setId(null);
        step.setTask(task);
        step.setStepOrder(nextStepOrder);
        if (task.getSteps() == null) {
            task.setSteps(new ArrayList<>());
        }
        task.getSteps().add(step);
        Step savedStep = stepRepository.save(step);

        log.info("Step created: taskId={}, stepId={}, stepOrder={}", taskId,
                                                                                savedStep.getId(),
                                                                                savedStep.getStepOrder());

        return savedStep;
    }

    @Override
    public List<Step> findStepsByTaskId(Long taskId) {
        findTaskByIdOrThrow(taskId);

        return stepRepository.findByTaskIdOrderByStepOrderAsc(taskId);
    }

    @Override
    public Step findStepById(Long taskId, Long stepId) {

        return findStepByIdOrThrow(taskId, stepId);
    }

    @Override
    @Transactional
    public Step updateStep(Long taskId, Long stepId, Step updatedStep) {
        Step existingStep = findStepByIdOrThrow(taskId, stepId);
        existingStep.setDescription(updatedStep.getDescription());
        Step savedStep = stepRepository.save(existingStep);
        log.info("Step updated: taskId={}, stepId={}", taskId, stepId);

        return savedStep;
    }

    @Override
    @Transactional
    public void deleteStep(Long taskId, Long stepId) {
        Step step = findStepByIdOrThrow(taskId, stepId);
        step.getTask().getSteps().remove(step);
        stepRepository.delete(step);
        log.info("Step deleted: taskId={}, stepId={}", taskId, stepId);
    }

    private Step findStepByIdOrThrow(Long taskId, Long stepId) {
        findTaskByIdOrThrow(taskId);

        return stepRepository.findByIdAndTaskId(stepId, taskId)
                .orElseThrow(() -> new StepNotFoundException(taskId, stepId));
    }

    private Task findTaskByIdOrThrow(Long id) {

        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

}
