/*
 * @ (#) Authority.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
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
@Table(name = "tbl_permission")
@Entity
public class Permission extends AbstractEntity<String> {
    private String permissionName;
    private String permissionKey;
}   
