package com.vivereply.roomready.data.sync

/**
 * Sends queued mutations to the live PMS.
 *
 * Implementations apply authentication (API key from the secure credential
 * store) at send time. [OutgoingMutation] never carries credentials.
 * Implementations throw on any transport or server failure so [SyncEngine]
 * can reschedule with backoff; a null/empty-body success deletes the entry.
 */
interface PmsTransport {
  suspend fun send(mutation: OutgoingMutation)
}
