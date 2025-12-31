package de.vantrex.skysens.client.enums.location.zone;

public interface ZoneEnum<T extends Enum<T>> {

    Class<T> getZoneClass();

    String getScoreboardName();

    String getDisplayName();

}
