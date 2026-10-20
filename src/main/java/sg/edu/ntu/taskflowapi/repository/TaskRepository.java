package sg.edu.ntu.taskflowapi.repository;

import sg.edu.ntu.taskflowapi.model.Task;
import java.util.List;
import java.util.Optional;


public interface TaskRepository {

    Optional<Task> findById(Long id);

    Task save(Task task);

    void deleteById(Long id);

    boolean existsById(Long id);

    List<Task> findAll();

}