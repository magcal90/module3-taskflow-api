package sg.edu.ntu.taskflowapi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotBlank;

/**
 * An ordered instruction that belongs to one task.
 */
@NoArgsConstructor
@Getter
@Setter
@JsonPropertyOrder({ "id", "description", "stepOrder" })
@Entity
@Table(name = "step", 
       uniqueConstraints = @UniqueConstraint(name = "uk_step_task_order", 
       columnNames = { "task_id", "step_order" }))
public class Step {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "description")
    @NotBlank(message = "Description is mandatory")
    private String description;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    private Task task;

}
