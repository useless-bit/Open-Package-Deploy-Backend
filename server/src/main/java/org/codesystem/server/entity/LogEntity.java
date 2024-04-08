package org.codesystem.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codesystem.server.converter.LogSeverityConverter;
import org.codesystem.server.converter.OperatingSystemConverter;
import org.codesystem.server.enums.log.Severity;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "log")
public class LogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", updatable = false, nullable = false)
    private String uuid;

    @CreationTimestamp
    private Instant timestamp;

    @Column(name = "severity", updatable = false, nullable = false)
    @Convert(converter = LogSeverityConverter.class)
    private Severity severity;

    @Column(name = "message", updatable = false, nullable = false, length = 1024)
    private String message;

    public LogEntity(Severity severity, String message) {
        this.severity = severity;
        this.message = message;
    }
}
