package com.dhj.actinium.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndPortalBlockEntityIdScopeTest {
    @Test
    void notifiesNeutralIdBeforeDrawAndRestoresPortalIdAfterward() {
        int[] currentId = {5025};
        List<Integer> notifications = new ArrayList<>();

        EndPortalBlockEntityIdScope.run(
            () -> currentId[0],
            id -> {
                currentId[0] = id;
                notifications.add(id);
            },
            () -> assertEquals(0, currentId[0])
        );

        assertEquals(List.of(0, 5025), notifications);
        assertEquals(5025, currentId[0]);
    }

    @Test
    void restoresPortalIdWhenDrawFails() {
        int[] currentId = {9000};
        List<Integer> notifications = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> EndPortalBlockEntityIdScope.run(
            () -> currentId[0],
            id -> {
                currentId[0] = id;
                notifications.add(id);
            },
            () -> {
                throw new IllegalStateException("draw failed");
            }
        ));

        assertEquals(List.of(0, 9000), notifications);
        assertEquals(9000, currentId[0]);
    }

    @Test
    void restoresPortalIdWhenNeutralIdNotificationFailsAfterAssignment() {
        int[] currentId = {119};
        List<Integer> notifications = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> EndPortalBlockEntityIdScope.run(
            () -> currentId[0],
            id -> {
                currentId[0] = id;
                notifications.add(id);
                if (id == 0) {
                    throw new IllegalStateException("neutral notification failed");
                }
            },
            () -> {
                throw new AssertionError("draw must not run after a notification failure");
            }
        ));

        assertEquals(List.of(0, 119), notifications);
        assertEquals(119, currentId[0]);
    }
}
