/**
 * Translation between Minecraft-coupled mod types and the plain DTOs in
 * {@code skysens-common}.
 *
 * <p>This package is the reason §4.4 could keep {@code Notification},
 * {@code DungeonStatParser} and the rendering-adjacent types in the mod. The rule
 * is one-directional and absolute: a Minecraft type is never dragged into
 * {@code skysens-common} to "make it fit" — it is mapped here instead.
 *
 * <p>Nothing lives here yet because zero features are migrated in this pass. The
 * first arrivals will be:
 * <ul>
 *   <li>{@code WaypointMapper} — {@code WaypointDto} to whatever position and
 *       colour types {@code BossWaypointsFeature} renders with.</li>
 *   <li>{@code NotificationPresetMapper} — {@code NotificationPresetDto} to
 *       {@code client.model.Notification}, which holds a {@code Component}.</li>
 * </ul>
 */
package de.vantrex.skysens.client.net.mapper;
