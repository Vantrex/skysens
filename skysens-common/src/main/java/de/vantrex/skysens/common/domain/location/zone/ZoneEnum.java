package de.vantrex.skysens.common.domain.location.zone;

public interface ZoneEnum<T extends Enum<T>> {

    Class<T> getZoneClass();

    String getScoreboardName();

    String getDisplayName();

    String name();

}
