package com.calaf.core.model

import kotlinx.coroutines.flow.StateFlow

data class AdminMember(val id: String, val email: String, val suspended: Boolean, val isAdmin: Boolean)
/** A photo in the moderation queue. */
data class AdminPhoto(val id: String, val ownerId: String, val ownerEmail: String, val state: PhotoState)
data class AdminReport(val id: String, val target: String, val details: String, val status: String)

/** Moderation tools. Only the demo flavour has an implementation for now. */
interface AdminRepository {
    /** Changes whenever the data changes, so screens can refresh. */
    val revision: StateFlow<Int>
    val isAdmin: Boolean

    fun members(): List<AdminMember>
    fun reports(): List<AdminReport>
    fun auditCount(): Int
    fun photos(): List<AdminPhoto>
    fun photoBytes(photoId: String): ByteArray

    /** Approves or rejects a photo. A rejected photo disappears from every other member's view. */
    fun reviewPhoto(photoId: String, approve: Boolean)
    fun suspend(id: String, suspended: Boolean)
    fun resolveReport(id: String, dismiss: Boolean)
}
