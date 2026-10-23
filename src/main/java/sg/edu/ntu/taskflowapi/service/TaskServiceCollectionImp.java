package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;
import sg.edu.ntu.taskflowapi.exceptions.StepNotFoundException;
import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/** Collection-backed task service selected by the {@code memory} profile. */
@Service
@Profile("memory")
@Slf4j

public class TaskServiceCollectionImp implements TaskService {

    private static final boolean COMPLETED = true;

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final AtomicLong stepIdGenerator = new AtomicLong(0);

    private final ChatClient chatClient;

    public TaskServiceCollectionImp(ChatClient chatClient) {

        this.chatClient = chatClient;

    }

    @Override
    public List<Task> findAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Task findTaskById(Long id) {
        return findTaskByIdOrThrow(id);
    }

    @Override
    public Task createTask(Task task) {

        Long id = idGenerator.incrementAndGet();
        task.setId(id);
        tasks.put(id, task);
        return task;
    }

    @Override
    public Task updateTask(Long id, Task task) {

        Task existingTask = findTaskByIdOrThrow(id);

        existingTask.setTitle(task.getTitle());
        existingTask.setNote(task.getNote());
        existingTask.setCompleted(task.isCompleted());
        existingTask.setDueDate(task.getDueDate());

        return existingTask;
    }

    @Override
    public void deleteTask(Long id) {

        findTaskByIdOrThrow(id);
        tasks.remove(id);
    }

    @Override
    public Task markTaskAsCompleted(Long id) {

        Task task = findTaskByIdOrThrow(id);
        task.setCompleted(COMPLETED);
        tasks.put(id, task);
        return task;
    }

    @Override
    public String getTaskSummary() {

        List<Task> taskList = findAllTasks();

        if (taskList.isEmpty()) {
            return "There are no tasks.";
        }

        final LocalDate today = LocalDate.now();

        String formattedTasks = taskList.stream()
                .map(task -> TaskSummaryHelper.formatTasks(task, today))
                .collect(Collectors.joining("\n"));

        log.info(
                "Generating task summary for {} tasks",
                taskList.size());

        return chatClient
                .prompt()
                .system(TaskSummaryHelper.SYSTEM_PROMPT)
                .user(TaskSummaryHelper.formatUserPrompt(formattedTasks))
                .call()
                .content();

    }

    @Override
    public List<Task> searchTasksByKeyword(String keyword) {
        return tasks.values().stream()
                .filter(matchesKeyword(keyword))
                .toList();
    }

    private Predicate<Task> matchesKeyword(String keyword) {
        String normalizedKeyword = keyword.toLowerCase();

        return task -> task.getTitle().toLowerCase().contains(normalizedKeyword)
                || (task.getNote() != null && task.getNote().toLowerCase().contains(normalizedKeyword));
    }

    @Override
    public List<Task> searchTasksByCompleted(boolean completed) {
        return tasks.values().stream()
                .filter(task -> task.isCompleted() == completed)
                .toList();
    }

    @Override
    public List<Task> searchTasksDueBefore(LocalDate dueDate) {
        return tasks.values().stream()
                .filter(task -> !task.isCompleted())
                .filter(task -> task.getDueDate() != null)
                .filter(task -> task.getDueDate().isBefore(dueDate))
                .sorted(Comparator.comparing(Task::getDueDate))
                .toList();
    }

    @Override
    public Step addStepToTask(Long taskId, Step step) {
        Task task = findTaskByIdOrThrow(taskId);
        List<Step> steps = getOrCreateSteps(task);

        int nextStepOrder = steps.stream()
                .map(Step::getStepOrder)
                .filter(stepOrder -> stepOrder != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        step.setId(stepIdGenerator.incrementAndGet());
        step.setTask(task);
        step.setStepOrder(nextStepOrder);
        steps.add(step);
        return step;
    }

    @Override
    public List<Step> findStepsByTaskId(Long taskId) {
        Task task = findTaskByIdOrThrow(taskId);
        return getOrCreateSteps(task).stream()
                .sorted(Comparator.comparing(Step::getStepOrder))
                .toList();
    }

    @Override
    public Step findStepById(Long taskId, Long stepId) {
        Task task = findTaskByIdOrThrow(taskId);
        return getOrCreateSteps(task).stream()
                .filter(step -> step.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new StepNotFoundException(taskId, stepId));
    }

    @Override
    public Step updateStep(Long taskId, Long stepId, Step step) {
        Step existingStep = findStepById(taskId, stepId);
        existingStep.setDescription(step.getDescription());
        return existingStep;
    }

    @Override
    public void deleteStep(Long taskId, Long stepId) {
        Task task = findTaskByIdOrThrow(taskId);
        Step step = findStepById(taskId, stepId);
        getOrCreateSteps(task).remove(step);
    }

    private List<Step> getOrCreateSteps(Task task) {
        if (task.getSteps() == null) {
            task.setSteps(new ArrayList<>());
        }
        return task.getSteps();
    }

    private Task findTaskByIdOrThrow(Long id) {

        Task task = tasks.get(id);

        if (task == null) {
            throw new TaskNotFoundException(id);
        }

        return task;
    }

}
