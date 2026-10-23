package sg.edu.ntu.taskflowapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.ntu.taskflowapi.model.Step;

import java.util.List;
import java.util.Optional;

/** Provides persistence and task-scoped queries for steps. */
public interface StepRepository extends JpaRepository<Step, Long>{

    /**
     * @param taskId the task ID
     * @return the task's steps in ascending order
     */
    List<Step> findByTaskIdOrderByStepOrderAsc(Long taskId);

    /**
     * @param stepId the step ID
     * @param taskId the owning task ID
     * @return the matching task-scoped step, if present
     */
    Optional<Step> findByIdAndTaskId(Long stepId, Long taskId);

    /**
     * @param taskId the task ID
     * @return the task's highest ordered step, if present
     */
    Optional<Step> findTopByTaskIdOrderByStepOrderDesc(Long taskId);
}
