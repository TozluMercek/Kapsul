package com.example.kapsul.capsule

import android.service.notification.NotificationListenerService

/**
 * Android only lets an app read what's playing (media sessions) if the user grants it
 * notification access. Kapsül reads nothing from notifications; this service only exists
 * so the context capsule can record the current song.
 */
class MediaListenerService : NotificationListenerService()
