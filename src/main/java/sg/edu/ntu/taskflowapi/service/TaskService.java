package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.model.Step;
import java.util.List;
import java.time.LocalDateTime;

public interface TaskService {

    List<Task> findAllTasks();
    Task findTaskById(Long id);
    Task createTask(Task task);
    Task updateTask(Long id, Task task);
    void deleteTask(Long id);
    Task markTaskAsCompleted(Long id);
    String getTaskSummary();

    List<Task> searchTasksByKeyword(String keyword);
    List<Task> searchTasksByCompleted(boolean completed);
    List<Task> searchTasksDueBefore(LocalDateTime dateTime);

    Step addStepToTask(Long id, Step step); 
    List<Step> findStepsByTaskId(Long taskId);
    Step findStepById(Long taskId, Long stepId);
    Step updateStep(Long taskId, Long stepId, Step step);
    void deleteStep(Long taskId, Long stepId);
    
}