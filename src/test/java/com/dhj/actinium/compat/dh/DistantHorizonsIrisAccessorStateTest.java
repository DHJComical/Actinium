package com.dhj.actinium.compat.dh;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DistantHorizonsIrisAccessorStateTest {
    @Test
    void inactiveOrMissingAccessorBehavesAsAbsent() {
        TestIrisAccessor accessor = new TestIrisAccessor(false);

        assertNull(DistantHorizonsIrisAccessorState.activeAccessor(null));
        assertNull(DistantHorizonsIrisAccessorState.activeAccessor(accessor));
    }

    @Test
    void activeAccessorIsPreserved() {
        TestIrisAccessor accessor = new TestIrisAccessor(true);

        assertSame(accessor, DistantHorizonsIrisAccessorState.activeAccessor(accessor));
    }

    @Test
    void shaderPackStateIsEvaluatedForEveryCall() {
        TestIrisAccessor accessor = new TestIrisAccessor(false);

        assertNull(DistantHorizonsIrisAccessorState.activeAccessor(accessor));

        accessor.shaderPackInUse = true;
        assertSame(accessor, DistantHorizonsIrisAccessorState.activeAccessor(accessor));

        accessor.shaderPackInUse = false;
        assertNull(DistantHorizonsIrisAccessorState.activeAccessor(accessor));
    }

    private static final class TestIrisAccessor implements IIrisAccessor {
        private boolean shaderPackInUse;

        private TestIrisAccessor(boolean shaderPackInUse) {
            this.shaderPackInUse = shaderPackInUse;
        }

        @Override
        public String getModName() {
            return "test";
        }

        @Override
        public boolean isShaderPackInUse() {
            return this.shaderPackInUse;
        }

        @Override
        public boolean isRenderingShadowPass() {
            return false;
        }
    }
}
