package org.codesystem.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "deployment")
public class DeploymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @ManyToOne(optional = false)
    private AgentEntity agentEntity;

    @ManyToOne(optional = false)
    private PackageEntity packageEntity;

    @Column(name = "deployed")
    private boolean deployed = false;

    @Column(name = "expected_return_value")
    private String expectedReturnValue;

    @Column(name = "return_value")
    private String returnValue;

    @Column(name = "last_deployment_timestamp")
    private Instant lastDeploymentTimestamp;

    public DeploymentEntity(AgentEntity agentEntity, PackageEntity packageEntity, String expectedReturnValue) {
        this.agentEntity = agentEntity;
        this.packageEntity = packageEntity;
        this.expectedReturnValue = expectedReturnValue;
    }
}
