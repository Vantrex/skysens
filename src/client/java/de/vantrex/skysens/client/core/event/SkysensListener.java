package de.vantrex.skysens.client.core.event;

@FunctionalInterface
public interface SkysensListener<E extends SkysensEvent> {

    void handle(E event);

}
