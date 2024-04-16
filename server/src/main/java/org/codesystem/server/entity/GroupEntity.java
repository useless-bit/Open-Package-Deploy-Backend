package org.codesystem.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "agent_group")
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @ManyToMany
    private List<AgentEntity> members = new ArrayList<>();

    @ManyToMany
    private List<PackageEntity> deployedPackages = new ArrayList<>(
    );

    public GroupEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void addMember(AgentEntity agentEntity) {
        if (!members.contains(agentEntity)) {
            members.add(agentEntity);
        }
    }

    public void removeMember(AgentEntity agentEntity) {
        members.remove(agentEntity);
    }


    public void addPackage(PackageEntity packageEntity) {
        if (!deployedPackages.contains(packageEntity)) {
            deployedPackages.add(packageEntity);
        }
    }

    public void removePackage(PackageEntity packageEntity) {
        deployedPackages.remove(packageEntity);
    }
}
