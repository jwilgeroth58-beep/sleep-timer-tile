package com.jwilgeroth.sleeptimertile

import android.service.notification.NotificationListenerService

/**
 * We don't actually want to READ notifications — we never override onNotification
 * Posted(). The only reason this service exists is that being a registered
 * (and user-enabled) NotificationListenerService is Android's required proof of
 * trust before it will hand us the list of active media sessions via
 * MediaSessionManager.getActiveSessions().
 *
 * So: empty on purpose. Its value is entirely in the manifest declaration + the
 * user flipping it on in settings.
 */
class MediaNotificationListener : NotificationListenerService()
