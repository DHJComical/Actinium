package com.dhj.actinium.compat.dh;

import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ActiniumDHIrisCompat {
    private static final Logger LOGGER = LogManager.getLogger("ActiniumDHCompat");
    private static boolean attemptedRegistration;

    private ActiniumDHIrisCompat() {
    }

    public static void registerAccessor() {
        if (attemptedRegistration) {
            return;
        }

        attemptedRegistration = true;
        try {
            if (ModAccessorInjector.INSTANCE.get(IIrisAccessor.class) == null) {
                ModAccessorInjector.INSTANCE.bind(IIrisAccessor.class, new ActiniumDHIrisAccessor());
                LOGGER.info("Registered Actinium Iris accessor for Distant Horizons");
            }
        } catch (IllegalStateException e) {
            LOGGER.debug("Distant Horizons Iris accessor is already registered", e);
        }
    }
}
