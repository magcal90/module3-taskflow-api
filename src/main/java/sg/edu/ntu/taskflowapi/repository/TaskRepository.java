package sg.edu.ntu.taskflowapi.repository;

import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByTitleContainingIgnoreCaseOrNoteContainingIgnoreCase(
            String titleKeyword,
            String noteKeyword);

    List<Task> findByCompleted(boolean completed);

    List<Task> findByCompletedFalseAndDueAtBeforeOrderByDueAtAsc(LocalDateTime dateTime);

}