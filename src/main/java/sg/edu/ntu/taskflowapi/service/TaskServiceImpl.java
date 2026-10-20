package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;
import sg.edu.ntu.taskflowapi.exceptions.TaskNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ChatClient chatClient;
    private static final boolean COMPLETED = true;
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    public TaskServiceImpl(TaskRepository taskRepository, ChatClient chatClient) {
        this.taskRepository = taskRepository;
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
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long id, Task task) {
        Task existingTask = findTaskByIdOrThrow(id);
        task.setId(existingTask.getId());
        return taskRepository.save(task);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = findTaskByIdOrThrow(id);
        taskRepository.deleteById(task.getId());

    }

    @Override
    public Task markTaskAsCompleted(Long id) {
        Task task = findTaskByIdOrThrow(id);
        task.setCompleted(COMPLETED);
        return taskRepository.save(task);
    }

    @Override
    public String getTaskSummary() {

        List<Task> tasks = taskRepository.findAll();

        if (tasks.isEmpty()) {
            return "There are no tasks.";
        }

        final LocalDateTime now = LocalDateTime.now();

        String taskList = tasks.stream()
                .map(task -> TaskSummaryHelper.formatTasks(task, now))
                .collect(Collectors.joining("\n"));

        logger.info("Generating task summary for {} tasks", tasks.size());

        try {
            return chatClient
                    .prompt()
                    .system(TaskSummaryHelper.SYSTEM_PROMPT)
                    .user(TaskSummaryHelper.formatUserPrompt(taskList))
                    .call()
                    .content();
        } catch (RuntimeException runtimeException) {
            logger.error("Failed to generate task summary", runtimeException);
            throw runtimeException;
        }
    }

    private Task findTaskByIdOrThrow(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

}
