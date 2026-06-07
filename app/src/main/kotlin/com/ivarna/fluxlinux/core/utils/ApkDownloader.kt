package com.ivarna.fluxlinux.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ApkDownloader {

    data class DownloadProgress(
        val progress: Float = 0f,
        val isDone: Boolean = false,
        val error: String? = null
    )

    private val client = OkHttpClient()

    fun download(context: Context, url: String, fileName: String): Flow<DownloadProgress> = flow {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            emit(DownloadProgress(error = "HTTP ${response.code}"))
            return@flow
        }

        val body = response.body ?: run {
            emit(DownloadProgress(error = "Empty response"))
            return@flow
        }

        val contentLength = body.contentLength()
        val file = File(context.cacheDir, fileName)

        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    totalBytes += bytes
                    val progress = if (contentLength > 0) totalBytes.toFloat() / contentLength else -1f
                    emit(DownloadProgress(progress = progress))
                }
            }
        }

        emit(DownloadProgress(progress = 1f, isDone = true))
    }.flowOn(Dispatchers.IO)

    fun getInstallIntent(context: Context, fileName: String): Intent? {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) return null

        val uri: Uri = FileProvider.getUriForFile(context, "com.ivarna.fluxlinux.provider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun deleteApk(context: Context, fileName: String): Boolean {
        return File(context.cacheDir, fileName).delete()
    }

    fun apkExists(context: Context, fileName: String): Boolean {
        return File(context.cacheDir, fileName).exists()
    }

    fun verifySha256(context: Context, fileName: String, expectedSha256: String): Boolean {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytes = fis.read(buffer)
                while (bytes > 0) {
                    digest.update(buffer, 0, bytes)
                    bytes = fis.read(buffer)
                }
            }
            val hashBytes = digest.digest()
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            hashString.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
