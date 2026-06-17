package zzp.tools.openlocalhost

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.net.toUri

class MainActivity : Activity() {

    companion object {
        private const val PREFS_NAME = "resolution_prefs"
        private const val KEY_ORIGINAL_SIZE = "original_size"
        private const val TARGET_SIZE = "720x1280"
        private const val TAG = "OpenLocalhost"
    }

    private var dialog: AlertDialog? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler.post { showMainDialog() }
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    /**
     * 显示主对话框：选择设置分辨率或恢复分辨率
     */
    private fun showMainDialog() {
        if (isFinishing || isDestroyed) return

        val originalSize = getSavedOriginalResolution()
        val currentPhysical = getPhysicalResolution()

        val message = buildString {
            append("当前物理分辨率: $currentPhysical\n")
            if (originalSize != null) {
                append("上次保存的原始分辨率: $originalSize\n")
            }
            append("\n请选择操作：")
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("分辨率管理")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("设置720x1280并启动") { _, _ ->
                // 保存原始分辨率
                if (currentPhysical != null) {
                    saveOriginalResolution(currentPhysical)
                }
                // 设置目标分辨率
                if (setResolution(TARGET_SIZE)) {
                    openBrowser()
                } else {
                    Toast.makeText(
                        this,
                        "设置分辨率失败，请先通过ADB授权：\nadb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS",
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
            .setNegativeButton("恢复原始分辨率") { _, _ ->
                if (originalSize != null) {
                    if (setResolution(originalSize)) {
                        clearSavedResolution()
                        Toast.makeText(this, "分辨率已恢复为 $originalSize", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "恢复失败", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (execCommand(arrayOf("wm", "size", "reset"))) {
                        Toast.makeText(this, "分辨率已重置", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "重置失败", Toast.LENGTH_LONG).show()
                    }
                }
                finish()
            }
            .setNeutralButton("直接启动") { _, _ ->
                openBrowser()
                finish()
            }
            .show()
    }

    /**
     * 打开浏览器
     */
    private fun openBrowser() {
        try {
            val url = "http://127.0.0.1:22267"
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 执行 shell 命令
     */
    private fun execCommand(args: Array<String>): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(args)
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            android.util.Log.d(TAG, "execCommand: ${args.joinToString(" ")}, exit=$exitCode, out=[$output], err=[$error]")
            exitCode == 0
        } catch (e: Exception) {
            android.util.Log.e(TAG, "execCommand exception: ${args.joinToString(" ")}", e)
            false
        }
    }

    /**
     * 设置分辨率
     */
    private fun setResolution(size: String): Boolean {
        return execCommand(arrayOf("wm", "size", size))
    }

    /**
     * 获取物理分辨率
     */
    private fun getPhysicalResolution(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("wm", "size"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            android.util.Log.d(TAG, "getPhysicalResolution: output=[$output]")
            val regex = Regex("Physical size: (\\d+x\\d+)")
            regex.find(output)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveOriginalResolution(size: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_ORIGINAL_SIZE, size)
            .apply()
        android.util.Log.d(TAG, "saveOriginalResolution: $size")
    }

    private fun getSavedOriginalResolution(): String? {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_ORIGINAL_SIZE, null)
    }

    private fun clearSavedResolution() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
    }
}
