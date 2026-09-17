/**
 * Push/pull scheduling for the API layer.
 *
 * <p>Pull tasks refresh {@code client/net/cache} (location assets, split
 * definitions, waypoint sets). Push tasks drain a local outbox (completed dungeon
 * runs) so a submission survives the server being down at the moment the run ended.
 *
 * <p>Everything here runs on {@code SkysensClient.SCHEDULER}. Nothing here may be
 * awaited from a feature.
 */
package de.vantrex.skysens.client.net.sync;
