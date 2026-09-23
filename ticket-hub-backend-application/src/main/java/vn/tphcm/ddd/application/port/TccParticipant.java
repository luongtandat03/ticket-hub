/*
 * @ (#) TccParticipant.java       1.0     9/10/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.port;

/*
 * @author: Luong Tan Dat
 * @date: 9/10/2026
 */
public interface TccParticipant {
    String tryPhase(TccContext context);
    void confirm(TccContext context);
    void cancel(TccContext context);
}
