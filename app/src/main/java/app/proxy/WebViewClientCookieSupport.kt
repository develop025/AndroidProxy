package app.proxy

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class WebViewClientCookieSupport(
    private val activity: Activity
) : WebViewClient() {

    private var fileUrl: String? = null

    companion object {
        const val WRITE_EXTERNAL_STORAGE = 0
    }


    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            overloadUrl(request?.url.toString())
                ?: super.shouldOverrideUrlLoading(view, request)
        else
            super.shouldOverrideUrlLoading(view, request)
    }

    private fun overloadUrl(
        url: String?
    ): Boolean? {
        return when {
            url == null -> null
            url.startsWith("mailto") -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "plain/text"
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(url.replace("mailto:", "")))
                activity.startActivity(Intent.createChooser(intent, "Mail to Support"))
                true
            }
            url.startsWith("tel:") -> {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse(url)
                activity.startActivity(intent)
                true
            }
            (url.startsWith("https://t.me") ||
                    url.startsWith("https://invite.viber.com") ||
                    url.startsWith("https://www.instagram.com") ||
                    url.startsWith("https://vk.com") ||
                    url.contains("rm_reg") ||
                    url.contains("rm_get_app")) -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                true
            }
            //todo need?
            url.endsWith(".apk") -> {
                startDownload(url)
                true
            }
            else -> null
        }
    }

    private fun startDownload(fileUrl: String) {
        this.fileUrl = fileUrl

        val permissionCheck =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) downloadFile()
        else ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            WRITE_EXTERNAL_STORAGE
        )
    }

    private fun downloadFile() {
        fileUrl?.let {
            val downloadManager =
                activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(it)
            val request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setVisibleInDownloadsUi(true)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                uri.lastPathSegment
            )
            downloadManager.enqueue(request)
        }
    }
}