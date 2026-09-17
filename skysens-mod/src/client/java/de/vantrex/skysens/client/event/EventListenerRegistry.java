package de.vantrex.skysens.client.event;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventListenerRegistry {

    public static final EventListenerRegistry REGISTRY = new EventListenerRegistry();

    private final Map<Class<? extends SkysensEvent>, List<SkysensListener<? extends SkysensEvent>>> listeners = new HashMap<>();

    public <T extends SkysensEvent> void registerListener(SkysensListener<T> listener, Class<T> eventClass) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    public void fireEvent(final @NotNull SkysensEvent event) {
        List<SkysensListener<? extends SkysensEvent>> listenerCollection = listeners.get(event.getClass());
        if (listenerCollection == null) {
            return;
        }
        listenerCollection.forEach(skysensListener -> {
            SkysensListener<SkysensEvent> castedListener = (SkysensListener<SkysensEvent>) skysensListener;
            this.fireListener(castedListener, event);
        });

    }

    private <E extends SkysensEvent, T extends SkysensListener<E>> void fireListener(T t, @NotNull E event) {
        t.handle(event);
    }

}
