package de.vantrex.skysens.client.feature;

public interface Feature {

    boolean isActive();

    default int priority() {
        return 0;
    }

}
