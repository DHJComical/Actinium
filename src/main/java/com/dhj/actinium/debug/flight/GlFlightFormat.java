package com.dhj.actinium.debug.flight;

final class GlFlightFormat {
    static final long MAGIC = 0x31474F4C46544341L;
    static final int VERSION = 1;
    static final int HEADER_SIZE = 128;
    static final int SLOT_SIZE = 96;

    static final int HEADER_MAGIC = 0;
    static final int HEADER_VERSION = 8;
    static final int HEADER_SIZE_OFFSET = 12;
    static final int HEADER_SLOT_SIZE = 16;
    static final int HEADER_SLOT_COUNT = 20;
    static final int HEADER_MODE = 24;
    static final int HEADER_CLEAN_CLOSE = 28;
    static final int HEADER_PROCESS_ID = 32;
    static final int HEADER_START_EPOCH_MILLIS = 40;
    static final int HEADER_START_NANOS = 48;

    static final int SLOT_COMMIT_SEQUENCE = 0;
    static final int SLOT_EPOCH_MILLIS = 8;
    static final int SLOT_NANOS = 16;
    static final int SLOT_FRAME = 24;
    static final int SLOT_THREAD_ID = 32;
    static final int SLOT_ARGUMENT_0 = 40;
    static final int SLOT_ARGUMENT_1 = 48;
    static final int SLOT_ARGUMENT_2 = 56;
    static final int SLOT_ARGUMENT_3 = 64;
    static final int SLOT_KIND = 72;
    static final int SLOT_PHASE = 76;
    static final int SLOT_STAGE = 80;

    private GlFlightFormat() {
    }

    static long fileSize(int slotCount) {
        return Math.addExact(HEADER_SIZE, Math.multiplyExact((long) SLOT_SIZE, slotCount));
    }

    static long slotOffset(int slotIndex) {
        return HEADER_SIZE + (long) SLOT_SIZE * slotIndex;
    }
}
