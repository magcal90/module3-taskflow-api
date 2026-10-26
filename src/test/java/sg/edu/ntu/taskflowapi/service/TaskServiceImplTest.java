package sg.edu.ntu.taskflowapi.service;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sg.edu.ntu.taskflowapi.repository.TaskRepository;
import sg.edu.ntu.taskflowapi.model.Task;

@ExtendWith(MockitoExtension.class)
public class TaskServiceImplTest {
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createTask_validTask_returnsSavedTask() {

        // ARRANGE
        Task inputTask = new Task();
        inputTask.setTitle("Submit assignment");
        inputTask.setCompleted(false);

        Task savedTask = new Task();
        savedTask.setId(99L);
        savedTask.setTitle("Submit assignment");
        savedTask.setCompleted(false);

        when(taskRepository.save(inputTask)).thenReturn(savedTask);

        // ACT
        Task result = taskService.createTask(inputTask);

        // ASSERT
        assertEquals(99L, result.getId());
        assertEquals("Submit assignment", result.getTitle());
        assertFalse(result.isCompleted());
        verify(taskRepository, times(1)).save(inputTask);

    }

    @Test
    void findTaskById_existingId_returnsTask() {
        // ARRANGE
        Task expectedTask = new Task();
        expectedTask.setId(99L);
        expectedTask.setTitle("Submit assignment");

        when(taskRepository.findById(99L))
                .thenReturn(Optional.of(expectedTask));

        // ACT
        Task result = taskService.findTaskById(99L);

        // ASSERT
        assertEquals(99L, result.getId());
        assertEquals("Submit assignment", result.getTitle());
        verify(taskRepository, times(1)).findById(99L);
    }

}
