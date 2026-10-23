/*
 * @ (#) Conversation.java       1.0     10/16/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 10/16/2026
 */

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_conversation")
public class Conversation extends AbstractEntity<String> {
    private String userId;

    private int status;

    private String assignedAgentId;
}
