package de.vantrex.skysens.client.features.macro.action;

import java.util.HashMap;
import java.util.Map;

/** Per action node cooldowns (key = workflowId:nodeId). Not persisted. */
public final class CooldownTracker {

    private static final CooldownTracker INSTANCE = new CooldownTracker();

    private final Map<String, Long> nextAllowedAt = new HashMap<>();

    public static CooldownTracker getInstance() {
        return INSTANCE;
    }

    public static String key(String workflowId, String nodeId) {
        return workflowId + ":" + nodeId;
    }

    public long remaining(String key) {
        final Long until = this.nextAllowedAt.get(key);
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    public void start(String key, long cooldownMs) {
        if (cooldownMs > 0) {
            this.nextAllowedAt.put(key, System.currentTimeMillis() + cooldownMs);
        }
    }

}
