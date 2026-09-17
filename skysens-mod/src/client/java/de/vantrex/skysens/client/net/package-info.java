/**
 * Client-side connection layer for the optional Skysens backend.
 *
 * <p><strong>Status: base plumbing only.</strong> This package holds the HTTP
 * client, the session/identity holder and the path constants. Nothing is wired
 * into {@code SkysensClient.onInitializeClient()}, no feature calls it, and every
 * method body carries a {@code TODO(deferred)} marker. Caching, sync scheduling and
 * DTO mapping will arrive alongside the first feature that actually needs them.
 *
 * <h2>Degradation contract</h2>
 *
 * This is the load-bearing rule and it should survive whatever gets built on top.
 * Offline-first is a hard requirement: every existing feature must work completely
 * with the server unreachable, returning 500s, or not configured. That is
 * achievable only if no feature ever awaits a network result.
 *
 * <table border="1">
 *   <caption>What a caller sees in each failure mode</caption>
 *   <tr><th>Condition</th><th>Behaviour</th></tr>
 *   <tr>
 *     <td>Server disabled in config (the default)</td>
 *     <td>No request is constructed at all. {@link de.vantrex.skysens.client.net.SkysensApiClient}
 *         short-circuits and returns an already-completed empty result. Zero cost.</td>
 *   </tr>
 *   <tr>
 *     <td>Base URL blank or malformed</td>
 *     <td>Treated exactly as disabled. Logged once at startup, never per request — a
 *         chat-spamming mod is a behavioural regression.</td>
 *   </tr>
 *   <tr>
 *     <td>Connect or read timeout</td>
 *     <td>The {@code CompletableFuture} completes empty. Whatever local value the
 *         caller already had stays in place and the feature never notices.</td>
 *   </tr>
 *   <tr>
 *     <td>5xx</td>
 *     <td>Same as a timeout. An outage must be indistinguishable from "offline".</td>
 *   </tr>
 *   <tr>
 *     <td>4xx other than 401</td>
 *     <td>Empty result, and the request is <em>not</em> retried — the client is
 *         wrong, not the server.</td>
 *   </tr>
 *   <tr>
 *     <td>401</td>
 *     <td>Drop the session token and attempt one re-handshake. A second 401 disables
 *         authenticated calls for the rest of the session.</td>
 *   </tr>
 *   <tr>
 *     <td>Version mismatch (426)</td>
 *     <td>Permanently disable backend calls for this session. Never retry: the
 *         answer will not change until the user updates.</td>
 *   </tr>
 * </table>
 *
 * <h2>Threading</h2>
 *
 * All network work runs on {@code SkysensClient.SCHEDULER} (the existing shared
 * 2-thread pool). No feature may create its own executor. Because that pool is
 * small and shared, every request carries a hard timeout. Results are applied back
 * on the client thread via {@code Minecraft.getInstance().execute(...)}; nothing in
 * this package touches game state directly.
 */
package de.vantrex.skysens.client.net;
