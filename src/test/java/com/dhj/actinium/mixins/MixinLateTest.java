package com.dhj.actinium.mixins;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MixinLateTest {
    @Test
    void selectsExactConfigsForLoadedMods() {
        assertEquals(List.of(), MixinLate.configsFor(modId -> false));

        assertEquals(
            List.of("mixins.actinium.lumenized.json"),
            MixinLate.configsFor("lumenized"::equals)
        );

        assertEquals(
            List.of(
                "mixins.actinium.dh.json",
                "mixins.actinium.gibbed.json",
                "mixins.actinium.lumenized.json"
            ),
            MixinLate.configsFor(modId -> true)
        );
    }
}
