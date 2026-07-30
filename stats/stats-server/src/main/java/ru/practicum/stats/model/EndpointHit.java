package ru.practicum.stats.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "endpoint_hits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String app;

    @Column(nullable = false, length = 1024)
    private String uri;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}