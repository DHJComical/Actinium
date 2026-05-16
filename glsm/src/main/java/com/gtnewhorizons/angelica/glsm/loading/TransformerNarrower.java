package com.gtnewhorizons.angelica.glsm.loading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TransformerNarrower {

    private static final Logger LOGGER = LogManager.getLogger("GLSM");

    public record NarrowRule(String name, String broad, List<String> narrow) {}

    public static int narrow(Set<String> exceptions, Map<String, Object> blackboard,
                             String keyPrefix, String callerName, List<NarrowRule> rules) {
        int applied = 0;
        for (NarrowRule rule : rules) {
            String key = keyPrefix + ".narrow." + rule.name();
            if (Boolean.FALSE.equals(blackboard.get(key))) {
                continue;
            }
            if (exceptions.remove(rule.broad())) {
                for (String narrow : rule.narrow()) {
                    exceptions.add(narrow);
                }
                LOGGER.info("[{}] Narrowed {} transformer exclusion to allow GL redirection", callerName, rule.broad());
                applied++;
            }
        }
        return applied;
    }

    private TransformerNarrower() {}
}
