package sg.edu.ntu.taskflowapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.repository.StepRepository;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "jpa" })
@Transactional
class StepApiIntegrationTest {

    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StepRepository stepRepository;

    @Test
    void createAndListStepsAssignsSequentialStepOrder() throws Exception {
        Task task = persistTask("Launch checklist");
        createStep(task.getId(), "Run integration tests", 201);
        createStep(task.getId(), "Publish release notes", 201);

        MvcResult result = mockMvc.perform(get("/api/tasks/{taskId}/steps", task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Run integration tests"))
                .andExpect(jsonPath("$[0].stepOrder").value(1))
                .andExpect(jsonPath("$[1].description").value("Publish release notes"))
                .andExpect(jsonPath("$[1].stepOrder").value(2))
                .andReturn();
        printResponse(result);
    }

    @Test
    void getStepReturnsRequestedStep() throws Exception {
        Task task = persistTask("Onboarding");
        long stepId = createStep(task.getId(), "Create account", 201);

        MvcResult result = mockMvc.perform(get("/api/tasks/{taskId}/steps/{stepId}", task.getId(), stepId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stepId))
                .andExpect(jsonPath("$.description").value("Create account"))
                .andExpect(jsonPath("$.stepOrder").value(1))
                .andReturn();
        printResponse(result);
    }

    @Test
    void updateStepChangesDescriptionButPreservesOrder() throws Exception {
        Task task = persistTask("Deployment");
        long stepId = createStep(task.getId(), "Deploy staging", 201);

        MvcResult result = mockMvc.perform(put("/api/tasks/{taskId}/steps/{stepId}", task.getId(), stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepJson("Deploy to production")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Deploy to production"))
                .andExpect(jsonPath("$.stepOrder").value(1))
                .andReturn();
        printResponse(result);
    }

    @Test
    void deleteStepRemovesIt() throws Exception {
        Task task = persistTask("Cleanup");
        long stepId = createStep(task.getId(), "Remove temporary files", 201);

        MvcResult deleted = mockMvc.perform(delete("/api/tasks/{taskId}/steps/{stepId}", task.getId(), stepId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();
        printResponse(deleted);

        MvcResult missing = mockMvc.perform(get("/api/tasks/{taskId}/steps/{stepId}", task.getId(), stepId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Step " + stepId + " was not found")))
                .andReturn();
        printResponse(missing);
    }

    @Test
    void createStepReturnsNotFoundWhenTaskIsMissing() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks/{taskId}/steps", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepJson("Cannot be attached")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found: " + MISSING_ID))
                .andReturn();
        printResponse(result);
    }

    @Test
    void getStepReturnsNotFoundWhenStepIsMissing() throws Exception {
        Task task = persistTask("Task with no steps");

        MvcResult result = mockMvc.perform(get("/api/tasks/{taskId}/steps/{stepId}", task.getId(), MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Step " + MISSING_ID + " was not found")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();
        printResponse(result);
    }

    @Test
    void getStepReturnsTaskNotFoundWhenParentTaskIsMissing() throws Exception {
        assertMissingParent(mockMvc.perform(
                get("/api/tasks/{taskId}/steps/{stepId}", MISSING_ID, MISSING_ID)).andReturn());
    }

    @Test
    void updateStepReturnsTaskNotFoundWhenParentTaskIsMissing() throws Exception {
        assertMissingParent(mockMvc.perform(
                put("/api/tasks/{taskId}/steps/{stepId}", MISSING_ID, MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepJson("Cannot be updated")))
                .andReturn());
    }

    @Test
    void deleteStepReturnsTaskNotFoundWhenParentTaskIsMissing() throws Exception {
        assertMissingParent(mockMvc.perform(
                delete("/api/tasks/{taskId}/steps/{stepId}", MISSING_ID, MISSING_ID)).andReturn());
    }

    @Test
    void deletingTaskAlsoDeletesItsSteps() throws Exception {
        Task task = persistTask("Task with cascading steps");
        long stepId = createStep(task.getId(), "Delete me with my task", 201);

        mockMvc.perform(delete("/api/tasks/{id}", task.getId()))
                .andExpect(status().isNoContent());
        taskRepository.flush();

        org.assertj.core.api.Assertions.assertThat(taskRepository.findById(task.getId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(stepRepository.findById(stepId)).isEmpty();
    }

    @Test
    void updateStepReturnsNotFoundWhenStepBelongsToAnotherTask() throws Exception {
        Task firstTask = persistTask("First task");
        Task secondTask = persistTask("Second task");
        long stepId = createStep(firstTask.getId(), "First task step", 201);

        MvcResult result = mockMvc.perform(put("/api/tasks/{taskId}/steps/{stepId}", secondTask.getId(), stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepJson("Cross-task update")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Step " + stepId + " was not found")))
                .andReturn();
        printResponse(result);
    }

    @Test
    void createStepRejectsBlankDescription() throws Exception {
        Task task = persistTask("Validation task");

        MvcResult result = mockMvc.perform(post("/api/tasks/{taskId}/steps", task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepJson("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Description is mandatory")))
                .andReturn();
        printResponse(result);
    }

    private Task persistTask(String title) {
        Task task = new Task();
        task.setTitle(title);
        return taskRepository.saveAndFlush(task);
    }

    private long createStep(long taskId, String description, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks/{taskId}/steps", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepJson(description)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        printResponse(result);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String stepJson(String description) {
        return objectMapper.writeValueAsString(Map.of("description", description));
    }

    private void assertMissingParent(MvcResult result) throws Exception {
        printResponse(result);
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .contains("Task not found: " + MISSING_ID)
                .contains("timestamp");
    }

    private void printResponse(MvcResult result) throws Exception {
        System.out.println("Response Code: " + result.getResponse().getStatus());
        System.out.println("Response Body: " + result.getResponse().getContentAsString());
    }
}
