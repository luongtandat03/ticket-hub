/*
 * @ (#) role.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_role")
public class Role extends AbstractEntity<String> {
    private String name;

    private String key;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tbl_role_has_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private List<Permission> permissions;
}
