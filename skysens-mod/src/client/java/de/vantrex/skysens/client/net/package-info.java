/**
 * Client-side API layer. Sits <em>beside</em> {@code client/repository}, not in
 * front of it: the repository layer remains the source of truth the features read
 * from, and this package is only ever an asynchronous refresher of that cache.
 *
 * <h2>Degradation contract</h2>
 *
 * This is the load-bearing part of the design and it is not negotiable. Constraint 4
 * says every existing feature must work completely with the server unreachable,
 * returning 500s, or not configured. That is achievable only if no feature ever
 * awaits a network result.
 *
 * <table border="1">
 *   <caption>What a feature does in each failure mode</caption>
 *   <tr><th>Condition</th><th>Behaviour</th></tr>
 *   <tr>
 *     <td>Server disabled in config (the default)</td>
 *     <td>No request is constructed at all. {@link de.vantrex.skysens.client.net.SkysensApiClient}
 *         short-circuits and returns an already-completed empty result. Zero cost.</td>
 *   </tr>
 *   <tr>
 *     <td>Base URL blank or malformed</td>
 *     <td>Treated exactly as disabled. Logged once at startup via {@code ClientUtil.sendDebug},
 *         never per request — a chat-spamming mod is a behavioural regression.</td>
 *   </tr>
 *   <tr>
 *     <td>Connect or read timeout</td>
 *     <td>The {@code CompletableFuture} completes empty. The cached value stays in
 *         place and the feature never notices. The sync layer backs off
 *         exponentially; it does not retry inside the request.</td>
 *   </tr>
 *   <tr>
 *     <td>5xx</td>
 *     <td>Same as a timeout: empty result, cache untouched, exponential backoff.
 *         An outage must be indistinguishable from "offline" to a feature.</td>
 *   </tr>
 *   <tr>
 *     <td>4xx other than 401</td>
 *     <td>Empty result and the request is <em>not</em> retried — the client is wrong,
 *         not the server.</td>
 *   </tr>
 *   <tr>
 *     <td>401</td>
 *     <td>Drop the session token and attempt one re-handshake. A second 401 disables
 *         authenticated sync for the rest of the session; the anonymous surface
 *         keeps working.</td>
 *   </tr>
 *   <tr>
 *     <td>Version mismatch (426, or manifest outside the supported window)</td>
 *     <td>Permanently disable all sync for this session and surface a single
 *         "Skysens server requires a newer mod version" debug line. Never retry:
 *         the answer will not change until the user updates.</td>
 *   </tr>
 * </table>
 *
 * <h2>Threading</h2>
 *
 * All network work runs on {@code SkysensClient.SCHEDULER} (the existing shared
 * 2-thread pool). No feature may create its own executor. Results are applied back
 * on the client thread via {@code Minecraft.getInstance().execute(...)}; nothing in
 * this package touches game state directly.
 *
 * <p><strong>Status: skeleton.</strong> Nothing here is wired into
 * {@code SkysensClient.onInitializeClient()}, and no feature calls into it. Zero
 * features are migrated to the server in this pass.
 */
package de.vantrex.skysens.client.net;
