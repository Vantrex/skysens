package de.vantrex.skysens.client.event;

@FunctionalInterface
public interface SkysensListener<E extends SkysensEvent> {

    void handle(E event);

}
