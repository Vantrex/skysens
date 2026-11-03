package de.vantrex.skysens.client.core.feature;

public interface Feature {

    boolean isActive();

    default int priority() {
        return 0;
    }

}
