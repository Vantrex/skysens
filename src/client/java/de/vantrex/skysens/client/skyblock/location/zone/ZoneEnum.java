package de.vantrex.skysens.client.skyblock.location.zone;

public interface ZoneEnum<T extends Enum<T>> {

    Class<T> getZoneClass();

    String getScoreboardName();

    String getDisplayName();

    String name();

}
