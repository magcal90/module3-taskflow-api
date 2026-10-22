package sg.edu.ntu.taskflowapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.ntu.taskflowapi.model.Step;

import java.util.List;
import java.util.Optional;

public interface StepRepository extends JpaRepository<Step, Long>{
    List<Step> findByTaskIdOrderByStepOrderAsc(Long taskId);

    Optional<Step> findByIdAndTaskId(Long stepId, Long taskId);

    Optional<Step> findTopByTaskIdOrderByStepOrderDesc(Long taskId);
}