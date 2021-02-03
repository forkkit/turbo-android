package dev.hotwire.turbo.delegates

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import dev.hotwire.turbo.R
import dev.hotwire.turbo.session.TurboSession
import dev.hotwire.turbo.util.TurboFileProvider
import dev.hotwire.turbo.util.TurboLog
import dev.hotwire.turbo.util.dispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal const val TURBO_REQUEST_CODE_FILES = 37

internal class TurboFileChooserDelegate(val session: TurboSession) : CoroutineScope {
    private val context: Context = session.context
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val browseFilesDelegate = TurboBrowseFilesDelegate(context)
    private val cameraCaptureDelegate = TurboCameraCaptureDelegate(context)

    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.io + Job()

    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        params: FileChooserParams
    ): Boolean {
        uploadCallback = filePathCallback

        return openChooser(params).also { success ->
            if (!success) handleCancellation()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != TURBO_REQUEST_CODE_FILES) return

        when (resultCode) {
            Activity.RESULT_OK -> handleResult(intent)
            Activity.RESULT_CANCELED -> handleCancellation()
        }
    }

    fun deleteCachedFiles() {
        launch {
            TurboFileProvider.deleteAllFiles(context)
        }
    }

    private fun openChooser(params: FileChooserParams): Boolean {
        val cameraIntent =  cameraCaptureDelegate.buildIntent(params)
        val chooserIntent = browseFilesDelegate.buildIntent(params)

        val mediaIntents = when (cameraIntent) {
            null -> emptyArray()
            else -> arrayOf(cameraIntent)
        }

        val intent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, chooserIntent)
            putExtra(Intent.EXTRA_TITLE, params.title())
            putExtra(Intent.EXTRA_INITIAL_INTENTS, mediaIntents)
        }

        return startIntent(intent)
    }

    private fun startIntent(intent: Intent): Boolean {
        val destination = session.currentVisitNavDestination ?: return false

        return try {
            destination.fragment.startActivityForResult(intent, TURBO_REQUEST_CODE_FILES)
            true
        } catch (e: Exception) {
            TurboLog.e("${e.message}")
            false
        }
    }

    private fun handleResult(intent: Intent?) {
        when (intent.containsFileResult()) {
            true -> browseFilesDelegate.handleResult(intent) {
                sendResult(it)
            }
            else -> cameraCaptureDelegate.handleResult {
                sendResult(it)
            }
        }
    }

    private fun sendResult(results: Array<Uri>?) {
        uploadCallback?.onReceiveValue(results)
        uploadCallback = null
    }

    private fun handleCancellation() {
        // Important to send a null value to the upload callback, otherwise the webview
        // gets into a state where it doesn't allow the file chooser to open again.
        uploadCallback?.onReceiveValue(null)
        uploadCallback = null
    }

    private fun FileChooserParams.title(): String {
        return title?.toString() ?: when (allowsMultiple()) {
            true -> session.context.getString(R.string.turbo_file_chooser_select_multiple)
            else -> session.context.getString(R.string.turbo_file_chooser_select)
        }
    }

    private fun Intent?.containsFileResult(): Boolean {
        return this?.dataString != null || this?.clipData != null
    }
}

internal fun FileChooserParams.allowsMultiple(): Boolean {
    return mode == FileChooserParams.MODE_OPEN_MULTIPLE
}

internal fun FileChooserParams.defaultAcceptType(): String {
    return when {
        acceptTypes.isEmpty() -> "*/*"
        acceptTypes.first().isBlank() -> "*/*"
        else -> acceptTypes.first()
    }
}