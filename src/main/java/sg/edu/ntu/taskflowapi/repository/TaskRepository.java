package sg.edu.ntu.taskflowapi.repository;

import sg.edu.ntu.taskflowapi.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


/** Provides persistence and derived searches for tasks. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * @param titleKeyword text to find in a title
     * @param noteKeyword text to find in a note
     * @return tasks matching either field, ignoring case
     */
    List<Task> findByTitleContainingIgnoreCaseOrNoteContainingIgnoreCase(
            String titleKeyword,
            String noteKeyword);

    /**
     * @param completed the required completion state
     * @return matching tasks
     */
    List<Task> findByCompleted(boolean completed);

    /**
     * @param dueDate the exclusive due-date cutoff
     * @return incomplete tasks before the cutoff in ascending due-date order
     */
    List<Task> findByCompletedFalseAndDueDateBeforeOrderByDueDateAsc(LocalDate dueDate);

}
