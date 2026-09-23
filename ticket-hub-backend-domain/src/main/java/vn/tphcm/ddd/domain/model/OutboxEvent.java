/*
 * @ (#) OutboxEvent.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.IdGeneratorType;
import vn.tphcm.ddd.domain.generator.UuidV7Generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_outbox_event", indexes = {
        @Index(name = "idx_status_created", columnList = "status, created_at")
})
@Builder
public class OutboxEvent {
    @Id
    @Uuidv7
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    private String aggregateId;

    private String eventType;

    private String payload;

    private int status;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    @IdGeneratorType(UuidV7Generator.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    @interface Uuidv7 {
    }
}
