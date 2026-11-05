package de.vantrex.skysens.client.config.categories.general;

/** What happens to a macro command when another one comes in while one waits for the transfer cooldown. */
public enum TransferCooldownQueueMode {

    /** Only the newest waiting command is kept. */
    KEEP_NEWEST,
    /** Every command waits and is sent one by one. */
    QUEUE_ALL;

}
