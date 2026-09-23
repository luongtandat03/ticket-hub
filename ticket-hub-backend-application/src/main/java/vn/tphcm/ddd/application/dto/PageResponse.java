/*
 * @ (#) PageResponse.java       1.0     9/7/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 9/7/2026
 */

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse <T>{
    private List<T> content;

    private Long nextCursor;

    private boolean hasMore;
}
