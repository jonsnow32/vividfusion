package cloud.app.vvf.ui.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import cloud.app.vvf.R
import cloud.app.vvf.utils.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ServicesSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.services)
  override val transitionName = "services_setting"
  override val container = { ServicesPreference() }

  val viewModel: ServicesViewModel by viewModels()

  inner class ServicesPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val ctx = preferenceManager.context
      preferenceManager.sharedPreferencesName = ctx.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(ctx)
      preferenceScreen = screen

      // ── Debrid Services ────────────────────────────────────────
      PreferenceCategory(ctx).apply {
        title = "Debrid Services"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)
      }

      // RealDebrid
      Preference(ctx).apply {
        title = "RealDebrid"
        summary = viewModel.rdSummary()
        key = "rd"
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(ctx, R.drawable.ic_extension_24dp)
        setOnPreferenceClickListener {
          showRDDialog(ctx)
          true
        }
        screen.addPreference(this)
      }

      // AllDebrid
      Preference(ctx).apply {
        title = "AllDebrid"
        summary = viewModel.adSummary()
        key = "ad"
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(ctx, R.drawable.ic_extension_24dp)
        setOnPreferenceClickListener {
          showADDialog(ctx)
          true
        }
        screen.addPreference(this)
      }

      // Premiumize
      Preference(ctx).apply {
        title = "Premiumize"
        summary = viewModel.pmSummary()
        key = "pm"
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(ctx, R.drawable.ic_extension_24dp)
        setOnPreferenceClickListener {
          showPMDialog(ctx)
          true
        }
        screen.addPreference(this)
      }

      // ── Subtitle Services ──────────────────────────────────────
      PreferenceCategory(ctx).apply {
        title = "Subtitle Services"
        isIconSpaceReserved = false
        layoutResource = R.layout.preference_category
        screen.addPreference(this)
      }

      // OpenSubtitles
      Preference(ctx).apply {
        title = "OpenSubtitles"
        summary = viewModel.osSummary()
        key = "os"
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(ctx, R.drawable.outline_subtitles_24)
        setOnPreferenceClickListener {
          showOSDialog(ctx)
          true
        }
        screen.addPreference(this)
      }
    }

    private fun refreshSummaries() {
      preferenceScreen?.apply {
        findPreference<Preference>("rd")?.summary = viewModel.rdSummary()
        findPreference<Preference>("ad")?.summary = viewModel.adSummary()
        findPreference<Preference>("pm")?.summary = viewModel.pmSummary()
        findPreference<Preference>("os")?.summary = viewModel.osSummary()
      }
    }

    // ── RealDebrid OAuth device flow ───────────────────────────────

    private fun showRDDialog(ctx: Context) {
      if (viewModel.isRDConnected()) {
        MaterialAlertDialogBuilder(ctx)
          .setTitle("RealDebrid")
          .setMessage("You are connected to RealDebrid.")
          .setNeutralButton("Disconnect") { _, _ ->
            viewModel.rdDisconnect()
            refreshSummaries()
          }
          .setPositiveButton("OK", null)
          .show()
        return
      }

      // Step 1: fetch device code
      val loadingDialog = MaterialAlertDialogBuilder(ctx)
        .setTitle("RealDebrid")
        .setMessage("Fetching auth code…")
        .setCancelable(false)
        .show()

      lifecycleScope.launch {
        val code = viewModel.rdStartAuth()
        loadingDialog.dismiss()

        if (code == null) {
          ctx.showToast("Failed to connect to RealDebrid")
          return@launch
        }

        // Step 2: show code to user
        MaterialAlertDialogBuilder(ctx)
          .setTitle("RealDebrid — Authorize")
          .setMessage(
            "1. Open the link below\n2. Enter the code shown\n\n" +
              "URL: ${code.direct_verification_url ?: code.verification_url}\n\n" +
              "Code: ${code.user_code}"
          )
          .setNeutralButton("Open browser") { _, _ ->
            val url = code.direct_verification_url ?: code.verification_url ?: return@setNeutralButton
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          }
          .setPositiveButton("Done — I've authorized") { _, _ ->
            pollRD(ctx, code.device_code)
          }
          .setNegativeButton("Cancel", null)
          .show()
      }
    }

    private fun pollRD(ctx: Context, deviceCode: String) {
      val polling = MaterialAlertDialogBuilder(ctx)
        .setTitle("RealDebrid")
        .setMessage("Verifying authorization…")
        .setCancelable(false)
        .show()

      lifecycleScope.launch {
        val ok = viewModel.rdPollCredentials(deviceCode)
        polling.dismiss()
        if (ok) {
          ctx.showToast("RealDebrid connected!")
          refreshSummaries()
        } else {
          ctx.showToast("Authorization not detected. Please try again.")
        }
      }
    }

    // ── AllDebrid PIN flow ─────────────────────────────────────────

    private fun showADDialog(ctx: Context) {
      if (viewModel.isADConnected()) {
        MaterialAlertDialogBuilder(ctx)
          .setTitle("AllDebrid")
          .setMessage("You are connected to AllDebrid.")
          .setNeutralButton("Disconnect") { _, _ ->
            viewModel.adDisconnect()
            refreshSummaries()
          }
          .setPositiveButton("OK", null)
          .show()
        return
      }

      val loading = MaterialAlertDialogBuilder(ctx)
        .setTitle("AllDebrid")
        .setMessage("Fetching PIN…")
        .setCancelable(false)
        .show()

      lifecycleScope.launch {
        val pin = viewModel.adStartAuth()
        loading.dismiss()

        if (pin?.data == null) {
          ctx.showToast("Failed to connect to AllDebrid")
          return@launch
        }

        val data = pin.data
        MaterialAlertDialogBuilder(ctx)
          .setTitle("AllDebrid — Authorize")
          .setMessage(
            "1. Open the link below\n2. Enter the PIN shown\n\n" +
              "URL: ${data.user_url}\n\nPIN: ${data.pin}"
          )
          .setNeutralButton("Open browser") { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(data.user_url)))
          }
          .setPositiveButton("Done — I've authorized") { _, _ ->
            pollAD(ctx, data.check, data.pin)
          }
          .setNegativeButton("Cancel", null)
          .show()
      }
    }

    private fun pollAD(ctx: Context, check: String, pin: String) {
      val polling = MaterialAlertDialogBuilder(ctx)
        .setTitle("AllDebrid")
        .setMessage("Verifying authorization…")
        .setCancelable(false)
        .show()

      lifecycleScope.launch {
        val ok = viewModel.adPollToken(check, pin)
        polling.dismiss()
        if (ok) {
          ctx.showToast("AllDebrid connected!")
          refreshSummaries()
        } else {
          ctx.showToast("Authorization not detected. Please try again.")
        }
      }
    }

    // ── Premiumize API key ─────────────────────────────────────────

    private fun showPMDialog(ctx: Context) {
      val currentKey = if (viewModel.isPMConnected()) "" else ""
      val editText = EditText(ctx).apply {
        hint = "Paste your Premiumize API key"
        if (viewModel.isPMConnected()) hint = "Current key hidden — paste new to replace"
        setSingleLine(true)
      }
      val container = LinearLayout(ctx).apply {
        setPadding(48, 16, 48, 0)
        addView(editText)
      }

      val builder = MaterialAlertDialogBuilder(ctx)
        .setTitle("Premiumize")
        .setView(container)
        .setPositiveButton("Save") { _, _ ->
          val key = editText.text.toString().trim()
          if (key.isNotEmpty()) {
            viewModel.pmSaveKey(key)
            refreshSummaries()
            ctx.showToast("Premiumize API key saved")
          }
        }
        .setNegativeButton("Cancel", null)

      if (viewModel.isPMConnected()) {
        builder.setNeutralButton("Disconnect") { _, _ ->
          viewModel.pmDisconnect()
          refreshSummaries()
        }
      }
      builder.show()
    }

    // ── OpenSubtitles API key ──────────────────────────────────────

    private fun showOSDialog(ctx: Context) {
      val editText = EditText(ctx).apply {
        hint = "OpenSubtitles API key (optional)"
        if (viewModel.isOSConfigured()) hint = "Current key hidden — paste new to replace"
        setSingleLine(true)
      }
      val container = LinearLayout(ctx).apply {
        setPadding(48, 16, 48, 0)
        addView(editText)
      }

      val builder = MaterialAlertDialogBuilder(ctx)
        .setTitle("OpenSubtitles API Key")
        .setMessage("Free key: register at opensubtitles.com (20 downloads/day). Without key: 5 downloads/day.")
        .setView(container)
        .setNeutralButton("Get free key") { _, _ ->
          startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.opensubtitles.com/consumers")))
        }
        .setPositiveButton("Save") { _, _ ->
          val key = editText.text.toString().trim()
          if (key.isNotEmpty()) {
            viewModel.osSaveKey(key)
            refreshSummaries()
            ctx.showToast("OpenSubtitles API key saved")
          }
        }
        .setNegativeButton("Cancel", null)

      if (viewModel.isOSConfigured()) {
        builder.setNeutralButton("Remove key") { _, _ ->
          viewModel.osClear()
          refreshSummaries()
        }
      }
      builder.show()
    }
  }
}
