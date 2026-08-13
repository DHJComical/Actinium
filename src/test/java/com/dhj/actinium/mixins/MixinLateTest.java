package com.dhj.actinium.mixins;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MixinLateTest {
    @Test
    void selectsExactConfigsForLoadedMods() {
        assertEquals(Set.of(), Set.copyOf(MixinLate.configsFor(modId -> false)));

        assertEquals(
            Set.of("mixins.actinium.betterfoliage.json"),
            Set.copyOf(MixinLate.configsFor("betterfoliage"::equals))
        );

        assertEquals(
            Set.of("mixins.actinium.ccl.json"),
            Set.copyOf(MixinLate.configsFor("codechickenlib"::equals))
        );

        assertEquals(
            Set.of(
                "mixins.actinium.dh.json",
                "mixins.actinium.gibbed.json",
                "mixins.actinium.ichunutil.json",
                "mixins.actinium.revoui.json",
                "mixins.actinium.betterfoliage.json",
                "mixins.actinium.ccl.json"
            ),
            Set.copyOf(MixinLate.configsFor(modId -> true))
        );
    }
}
