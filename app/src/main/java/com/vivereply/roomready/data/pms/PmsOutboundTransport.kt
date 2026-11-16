package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.sync.OutgoingMutation
import com.vivereply.roomready.data.sync.PmsTransport
import com.vivereply.roomready.security.SecureCredentialStore

/**
 * [PmsTransport] that signs each queued mutation at send time: the full key
 * is read from the vault only here, the actor is derived as
 * app:<keyId>:<staffName>, and anything missing aborts the drain via
 * [PmsAuthException] (no entry is at fault, so nothing parks).
 */
class PmsOutboundTransport(
  private val api: PmsApi,
  private val vault: SecureCredentialStore,
  private val conn: ConnectionStore,
  private val clock: Clock,
) : PmsTransport {

  override suspend fun send(mutation: OutgoingMutation) {
    val baseUrl = conn.baseUrl() ?: throw PmsAuthException("not connected")
    val fullKey = vault.apiKeyForAuth() ?: throw PmsAuthException("missing api key")
    val keyId = ApiKeyFormat.keyIdOf(fullKey) ?: throw PmsAuthException("malformed api key")
    val staffName = vault.staffIdentity()?.name ?: throw PmsAuthException("missing staff identity")
    api.push(
      PushRequest(
        baseUrl = baseUrl,
        apiKey = fullKey,
        entityType = mutation.entityType,
        entityId = mutation.entityId,
        op = mutation.op,
        payloadJson = mutation.payloadJson,
        actor = PmsContract.actorFor(keyId, staffName),
        atEpochMillis = clock.nowEpochMillis(),
      ),
    )
  }
}
