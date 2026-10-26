package sg.edu.ntu.taskflowapi.controller;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.model.Step;
import sg.edu.ntu.taskflowapi.repository.StepRepository;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "jpa" })
@Transactional
@Slf4j
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StepRepository stepRepository;

    @ParameterizedTest
    @CsvSource({
            "Pack luggage,Prepare clothes, toiletries and travel essentials for the Japan trip.",
            "Check in online, Complete airline online check-in when it becomes available.",
            "Set up eSIM, Purchase and activate a Japan mobile data plan before departure.",
            "Exchange currency, Prepare enough Japanese yen for the trip.",
            "Set out-of-office, Configure an automatic email reply for the travel period.",
            "Buy travel insurance, Purchase travel insurance before departure.",
            "Charge tablet, Fully charge the tablet before travelling."
    })
    void createTask_validTask_returnsCreated(String title, String note) throws Exception {
        final LocalDate dueDate = LocalDate.now().plusDays(5);
        final boolean INCOMPLETE = false;

        String serializedTask = serializeTask(title, note, INCOMPLETE, dueDate);
        RequestBuilder request = MockMvcRequestBuilders.post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedTask);

        // Step 4: Perform and assert
        // Step 4: Perform and assert
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.note").value(note))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
                .andExpect(jsonPath("$.steps").isArray())
                .andReturn();

        long taskId = json(result).get("id").asLong();
        Task savedTask = taskRepository.findById(taskId).orElseThrow();
        assertEquals(savedTask.getTitle(), title);
        assertEquals(savedTask.getNote(), note);
        assertEquals(savedTask.getDueDate(), dueDate);
        assertEquals(savedTask.isCompleted(), INCOMPLETE);
        printResponse(result);

    }

    @Test
    void addStep_packLuggage_returnsCreated() throws Exception {
        final LocalDate dueDate = LocalDate.now().plusDays(5);
        final String title = "Pack luggage";
        final String note = "Prepare clothes, toiletries and travel essentials for the Japan trip.";
        final String description = "Pack clothes and toiletries";

        String serializedTask = serializeTask(title, note, false, dueDate);
        MvcResult createdTask = mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedTask))
                .andExpect(status().isCreated())
                .andReturn();
        long taskId = json(createdTask).get("id").asLong();

        String serializedStep = objectMapper.writeValueAsString(Map.of("description", description));
        RequestBuilder request = MockMvcRequestBuilders.post("/api/tasks/{taskId}/steps", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedStep);

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.stepOrder").value(1))
                .andReturn();

        long stepId = json(result).get("id").asLong();
        Step savedStep = stepRepository.findById(stepId).orElseThrow();
        assertEquals(description, savedStep.getDescription());
        assertEquals(1, savedStep.getStepOrder());
        assertEquals(taskId, savedStep.getTask().getId());
        assertEquals(title, savedStep.getTask().getTitle());
        printResponse(result);
    }

    @Test
    void updateTask_exchangeCurrency_returnsUpdatedAndCompleted() throws Exception {
        final LocalDate dueDate = LocalDate.now().plusDays(5);
        final String note = "Prepare enough Japanese yen for the trip.";
        final String updatedTitle = "Top up YouTrip fund";
        final boolean COMPLETED = true;

        String serializedTask = serializeTask("Exchange currency", note, false, dueDate);
        MvcResult createdTask = mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedTask))
                .andExpect(status().isCreated())
                .andReturn();
        long taskId = json(createdTask).get("id").asLong();

        String updatedTask = serializeTask(updatedTitle, note, COMPLETED, dueDate);
        RequestBuilder request = MockMvcRequestBuilders.put("/api/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedTask);

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.note").value(note))
                .andExpect(jsonPath("$.completed").value(COMPLETED))
                .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
                .andExpect(jsonPath("$.steps").isArray())
                .andReturn();

        Task savedTask = taskRepository.findById(taskId).orElseThrow();
        assertEquals(updatedTitle, savedTask.getTitle());
        assertEquals(note, savedTask.getNote());
        assertEquals(dueDate, savedTask.getDueDate());
        assertEquals(COMPLETED, savedTask.isCompleted());
        printResponse(result);
    }

    private String serializeTask(String title, String note, boolean completed, LocalDate dueDate) throws Exception {
        Task task = new Task();
        task.setTitle(title);
        task.setNote(note);
        task.setCompleted(completed);
        task.setDueDate(dueDate);
        return objectMapper.writeValueAsString(task);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void printResponse(MvcResult result) throws Exception {
        log.info("Response Code: {}, Response Body: {}", result.getResponse().getStatus(),
                result.getResponse().getContentAsString());
    }
}
