package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;
import java.util.List;

public interface TaskService {

    List<Task> findAllTasks();

    Task findTaskById(Long id);

    Task createTask(Task task);

    Task updateTask(Long id, Task task);

    void deleteTask(Long id);

    Task markTaskAsCompleted(Long id);

    String getTaskSummary();

}