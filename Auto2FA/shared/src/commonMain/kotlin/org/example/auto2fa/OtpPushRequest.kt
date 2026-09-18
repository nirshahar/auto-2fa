package org.example.auto2fa

import kotlinx.serialization.Serializable

/** The JSON body of a `POST /push` request -- shared so the Android client and the desktop server agree on the exact wire shape. */
@Serializable
data class OtpPushRequest(val code: String)
