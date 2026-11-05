package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.config.SkysensConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Ring buffer of the most recent macro key presses. Not persisted. */
public final class ExecutionLog {

    private static final ExecutionLog INSTANCE = new ExecutionLog();

    private final Deque<ExecutionRecord> records = new ArrayDeque<>();

    public static ExecutionLog getInstance() {
        return INSTANCE;
    }

    public void add(ExecutionRecord record) {
        final int max = Math.max(1, (int) SkysensConfig.getInstance().macroCategory.executionLogSize);
        this.records.addFirst(record);
        while (this.records.size() > max) {
            this.records.removeLast();
        }
    }

    /** Newest first. */
    public List<ExecutionRecord> recent(int count) {
        final List<ExecutionRecord> out = new ArrayList<>();
        for (ExecutionRecord record : this.records) {
            if (out.size() >= count) {
                break;
            }
            out.add(record);
        }
        return out;
    }

}
