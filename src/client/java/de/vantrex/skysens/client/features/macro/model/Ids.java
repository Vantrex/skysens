package de.vantrex.skysens.client.features.macro.model;

import java.security.SecureRandom;

/** Short random ids ("n_aB3dE9xZ") for workflows, nodes, rules and connections. */
public final class Ids {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static String next(String prefix) {
        final StringBuilder sb = new StringBuilder(prefix).append('_');
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

}
