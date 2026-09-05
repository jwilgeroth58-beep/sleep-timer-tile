package com.jwilgeroth.sleeptimertile

import android.app.admin.DeviceAdminReceiver

/**
 * The Device Admin component. Registering the app as a "device admin" is the
 * only way (without root) to call DevicePolicyManager.lockNow() and turn the
 * screen off.
 *
 * We don't need to override anything: the mere existence of this receiver,
 * paired with res/xml/device_admin.xml declaring the <force-lock> policy and
 * the manifest entry, is what lets the user grant us screen-lock power. The
 * callbacks (onEnabled/onDisabled) are optional hooks we can add later if we
 * want to react to the permission being toggled.
 */
class SleepAdminReceiver : DeviceAdminReceiver()
