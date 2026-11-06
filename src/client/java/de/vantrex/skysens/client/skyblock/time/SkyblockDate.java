package de.vantrex.skysens.client.skyblock.time;

/**
 * A point in SkyBlock time. {@code day} is 1-31, {@code hour} 0-23 and {@code minute} 0-59 of the SkyBlock day
 * (the day is taken to start at 0:00; the offset to the in-game clock is not verified).
 */
public record SkyblockDate(int year, SkyblockMonth month, int day, int hour, int minute) {
}
