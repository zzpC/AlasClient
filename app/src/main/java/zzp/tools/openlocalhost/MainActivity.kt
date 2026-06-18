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
        private const val KEY_ORIGINAL_DENSITY = "original_density"
        private const val TARGET_SIZE = "720x1280"
        private const val TARGET_DENSITY = "320"
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

    private fun showMainDialog() {
        if (isFinishing || isDestroyed) return

        val originalSize = getSavedOriginalResolution()
        val originalDensity = getSavedOriginalDensity()
        val currentSize = getPhysicalResolution()
        val currentDensity = getPhysicalDensity()

        val message = buildString {
            append("物理分辨率: $currentSize\n")
            append("物理密度: $currentDensity\n")
            if (originalSize != null) append("保存的原始分辨率: $originalSize\n")
            if (originalDensity != null) append("保存的原始密度: $originalDensity\n")
            append("\n请选择操作：")
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("分辨率管理")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("设置720x1280/320并启动") { _, _ ->
                // 保存原始值
                currentSize?.let { saveOriginalResolution(it) }
                currentDensity?.let { saveOriginalDensity(it) }
                // 设置目标值
                val sizeOk = execRoot("wm size $TARGET_SIZE")
                val densityOk = execRoot("wm density $TARGET_DENSITY")
                // 开启 ADB 无线调试端口，方便本地 HTTP 服务通过回环地址访问
//                val adbdOk = execRoot("setprop service.adb.tcp.port 5555 && start adbd")
                android.util.Log.d(TAG, "size=$sizeOk, density=$densityOk")

                if (sizeOk && densityOk) {
                    openBrowser()
                } else {
                    Toast.makeText(this, "设置失败，请在KernelSU中授权root", Toast.LENGTH_LONG).show()
                }
                finish()
            }
            .setNegativeButton("恢复默认") { _, _ ->
                val sizeOk = execRoot("wm size reset")
                val densityOk = execRoot("wm density reset")
                android.util.Log.d(TAG, "reset size=$sizeOk, density=$densityOk")

                if (sizeOk && densityOk) {
                    clearSavedSettings()
                    Toast.makeText(this, "已重置为默认", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "恢复失败", Toast.LENGTH_LONG).show()
                }
                finish()
            }
            .setNeutralButton("直接启动") { _, _ ->
                openBrowser()
                finish()
            }
            .show()
    }

    private fun openBrowser() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "http://127.0.0.1:22267".toUri()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通过 su -c 执行 root 命令
     */
    private fun execRoot(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            android.util.Log.d(TAG, "execRoot: $command, exit=$exitCode, out=[$output], err=[$error]")
            exitCode == 0
        } catch (e: Exception) {
            android.util.Log.e(TAG, "execRoot exception: $command", e)
            false
        }
    }

    /**
     * 通过 su 获取 wm size 输出
     */
    private fun getPhysicalResolution(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "wm size"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            android.util.Log.d(TAG, "getPhysicalResolution: [$output]")
            Regex("Physical size: (\\d+x\\d+)").find(output)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通过 su 获取 wm density 输出
     */
    private fun getPhysicalDensity(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "wm density"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            android.util.Log.d(TAG, "getPhysicalDensity: [$output]")
            Regex("Physical density: (\\d+)").find(output)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveOriginalResolution(size: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(KEY_ORIGINAL_SIZE, size).apply()
    }

    private fun getSavedOriginalResolution(): String? =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_ORIGINAL_SIZE, null)

    private fun saveOriginalDensity(density: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(KEY_ORIGINAL_DENSITY, density).apply()
    }

    private fun getSavedOriginalDensity(): String? =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_ORIGINAL_DENSITY, null)

    private fun clearSavedSettings() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
    }
}
