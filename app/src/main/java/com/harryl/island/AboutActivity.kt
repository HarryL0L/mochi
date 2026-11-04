// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.harryl.island.databinding.ItemAboutRowBinding

/** About Mochi: what's new, privacy at a glance, phone details for bug reports, links, licences. */
class AboutActivity : AppCompatActivity() {

    private lateinit var privacy: LinearLayout
    private lateinit var phone: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.aboutToolbar)
            .setNavigationOnClickListener { finish() }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<NestedScrollView>(R.id.aboutScroll)) { v, insets ->
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }
        findViewById<TextView>(R.id.aboutVersion).text = getString(R.string.about_version, versionName(), versionCode())

        // A little hello when the page opens.
        val face = findViewById<MochiFaceView>(R.id.aboutFace)
        face.postDelayed({ face.wink(bothEyes = false) }, 450)

        buildWhatsNew(findViewById(R.id.groupNew))
        privacy = findViewById(R.id.groupPrivacy)
        phone = findViewById(R.id.groupPhone)
        buildLinks(findViewById(R.id.groupLinks))
        buildLicences(findViewById(R.id.groupLicences))
    }

    override fun onResume() {
        super.onResume()
        // Permissions can change while the user is away in Settings.
        buildPrivacy(privacy)
        buildPhone(phone)
    }

    // ---- Sections ------------------------------------------------------------------------------

    private fun buildWhatsNew(group: LinearLayout) {
        group.removeAllViews()
        val versions = resources.getStringArray(R.array.whats_new_versions)
        val notes = resources.getStringArray(R.array.whats_new_notes)
        versions.zip(notes).forEach { (v, n) -> row(group, v, n) }
        ExpressiveList.style(group)
    }

    private fun buildPrivacy(group: LinearLayout) {
        group.removeAllViews()
        row(group, getString(R.string.privacy_internet), getString(R.string.privacy_internet_body))
        row(group, getString(R.string.privacy_screen), getString(R.string.privacy_screen_body))
        val perms = buildList {
            add(R.string.perm_a11y to IslandAccessibilityService.isEnabled(this@AboutActivity))
            add(R.string.perm_notifications to NotificationManagerCompat.getEnabledListenerPackages(this@AboutActivity).contains(packageName))
            if (Build.VERSION.SDK_INT >= 31) add(R.string.perm_bluetooth to granted(Manifest.permission.BLUETOOTH_CONNECT))
            if (Build.VERSION.SDK_INT >= 33) add(R.string.perm_post to granted(Manifest.permission.POST_NOTIFICATIONS))
            add(R.string.perm_battery to getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))
            add(R.string.perm_overlay to Settings.canDrawOverlays(this@AboutActivity))
        }
        for ((title, on) in perms) {
            row(group, getString(title), null, getString(if (on) R.string.state_on else R.string.state_off)) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
        ExpressiveList.style(group)
    }

    private fun buildPhone(group: LinearLayout) {
        group.removeAllViews()
        val details = phoneDetails()
        for ((k, v) in details) row(group, k, v)
        row(group, getString(R.string.about_copy_details), getString(R.string.about_copy_details_body)) {
            val text = details.joinToString("\n") { (k, v) -> "$k: $v" }
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Mochi details", text))
            Toast.makeText(this, R.string.about_copied, Toast.LENGTH_SHORT).show()
        }
        ExpressiveList.style(group)
    }

    private fun buildLinks(group: LinearLayout) {
        group.removeAllViews()
        row(group, getString(R.string.link_source), REPO) { open(REPO) }
        row(group, getString(R.string.link_issue), getString(R.string.link_issue_body)) { open("$REPO/issues/new") }
        row(group, getString(R.string.link_updates), getString(R.string.link_updates_body)) { open("$REPO/tree/main/app/release") }
        ExpressiveList.style(group)
    }

    private fun buildLicences(group: LinearLayout) {
        group.removeAllViews()
        row(group, getString(R.string.licence_mochi), getString(R.string.licence_mochi_body)) {
            showLicence(R.string.licence_gpl, R.raw.gpl_3_0)
        }
        val libs = resources.getStringArray(R.array.licence_libraries)
        for (lib in libs) row(group, lib, getString(R.string.licence_apache)) { showLicence(R.string.licence_apache, R.raw.apache_2_0) }
        ExpressiveList.style(group)
    }

    // ---- Helpers -------------------------------------------------------------------------------

    /** Model, Android, Mochi and what Mochi knows about the screen and camera. */
    private fun phoneDetails(): List<Pair<String, String>> {
        val g = IslandGeometry.detect(this)
        val prefs = Prefs(this)
        fun box(b: CameraBox?) = b?.let { "${it.left},${it.top} – ${it.right},${it.bottom} px" } ?: getString(R.string.about_none)
        val mode = when (prefs.anchor) {
            Prefs.ANCHOR_CENTER -> getString(R.string.anchor_center)
            Prefs.ANCHOR_CALIBRATED -> getString(R.string.anchor_calibrated)
            else -> getString(R.string.anchor_auto)
        }
        return listOf(
            getString(R.string.detail_phone) to "${Build.MANUFACTURER} ${Build.MODEL}",
            getString(R.string.detail_android) to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            getString(R.string.detail_mochi) to "${versionName()} (${versionCode()})",
            getString(R.string.detail_screen) to "${g.screenW} × ${g.screenH} px, ${g.densityDpi} dpi",
            getString(R.string.detail_reported) to box(g.reported),
            getString(R.string.detail_camera) to "${box(g.camera)} · ${g.source.name.lowercase().replace('_', ' ')}",
            getString(R.string.detail_position) to "$mode · nudge ${prefs.offsetX}, ${prefs.offsetY} dp",
        )
    }

    private fun row(group: LinearLayout, title: String, body: String?, value: String? = null, onClick: (() -> Unit)? = null) {
        val r = ItemAboutRowBinding.inflate(LayoutInflater.from(this), group, true)
        r.rowTitle.text = title
        r.rowBody.text = body
        r.rowBody.visibility = if (body.isNullOrEmpty()) View.GONE else View.VISIBLE
        r.rowValue.text = value
        r.rowValue.visibility = if (value.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (onClick != null) r.root.setOnClickListener { onClick() } else r.root.isClickable = false
    }

    private fun showLicence(title: Int, raw: Int) {
        val text = resources.openRawResource(raw).bufferedReader().use { it.readText() }
        val pad = (20 * resources.displayMetrics.density).toInt()
        val tv = TextView(this).apply {
            this.text = text
            setTextIsSelectable(true)
            setPadding(pad, pad / 2, pad, pad)
            textSize = 12f
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(ScrollView(this).apply { addView(tv) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun open(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(this, url, Toast.LENGTH_LONG).show() }
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    private fun versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    private fun versionCode(): Long = packageManager.getPackageInfo(packageName, 0).longVersionCode

    companion object {
        const val REPO = "https://github.com/HarryL0L/mochi"
    }
}
