package com.example.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClipboardHelper(private val context: Context) {
  private val clipboardManager =
    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

  private val scope = CoroutineScope(Dispatchers.Main)
  private var autoClearJob: Job? = null

  private val _clipboardCountdown = MutableStateFlow<Int?>(null)
  val clipboardCountdown: StateFlow<Int?> = _clipboardCountdown.asStateFlow()

  fun copySensitive(text: String, label: String = "CipherVault Password", timeoutSeconds: Int = 30) {
    val clip = ClipData.newPlainText(label, text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      clip.description.extras = PersistableBundle().apply {
        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
      }
    }
    clipboardManager.setPrimaryClip(clip)

    // Schedule auto clear
    autoClearJob?.cancel()
    autoClearJob = scope.launch {
      for (remaining in timeoutSeconds downTo 1) {
        _clipboardCountdown.value = remaining
        delay(1000)
      }
      clearClipboard()
    }
  }

  fun clearClipboard() {
    autoClearJob?.cancel()
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        clipboardManager.clearPrimaryClip()
      } else {
        val emptyClip = ClipData.newPlainText("", "")
        clipboardManager.setPrimaryClip(emptyClip)
      }
    } catch (_: Exception) {
      // Fallback
    }
    _clipboardCountdown.value = null
  }
}
