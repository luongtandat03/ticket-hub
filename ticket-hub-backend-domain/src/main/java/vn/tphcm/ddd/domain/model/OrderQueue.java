/*
 * @ (#) OrderQueue.java       1.0     9/12/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 9/12/2026
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.IdGeneratorType;
import vn.tphcm.ddd.domain.generator.UuidV7Generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_order_queue")
@Builder
public class OrderQueue {
    @Id
    @Uuidv7
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    private String token;

    private String ticketId;

    private int quantity;

    private String userId;

    private int status;

    private String orderNumber;

    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @IdGeneratorType(UuidV7Generator.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    @interface Uuidv7 {
    }
}
