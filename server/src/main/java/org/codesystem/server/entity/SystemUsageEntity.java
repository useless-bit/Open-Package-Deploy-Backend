package org.codesystem.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "system_usage")
public class SystemUsageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @CreationTimestamp
    private Instant timestamp;

    @Column(name = "cpu_usage", updatable = false, nullable = false)
    private double cpuUsage;

    @Column(name = "memory_total", updatable = false, nullable = false)
    private long memoryTotal;

    @Column(name = "memory_available", updatable = false, nullable = false)
    private long memoryAvailable;

    public SystemUsageEntity(double cpuUsage, long memoryTotal, long memoryAvailable) {
        this.cpuUsage = cpuUsage;
        this.memoryTotal = memoryTotal;
        this.memoryAvailable = memoryAvailable;
    }
}
