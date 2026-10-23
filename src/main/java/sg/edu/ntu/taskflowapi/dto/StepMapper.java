package sg.edu.ntu.taskflowapi.dto;
import sg.edu.ntu.taskflowapi.model.Step;

public final class StepMapper {
    private StepMapper() {
        // class with static methods only, prevent instantiation
    }

    public static Step toStep(StepRequest stepRequest) {
        Step step = new Step();
        step.setDescription(stepRequest.description());
        return step;
    }
    public static StepResponse toStepResponse(Step step) {
        return new StepResponse(step.getId(), step.getDescription(), step.getStepOrder());
    }
}
