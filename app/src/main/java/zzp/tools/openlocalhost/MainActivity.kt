package zzp.tools.openlocalhost


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import kotlin.system.exitProcess

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // 1. 定义要打开的 URL
            val url = "http://127.0.0.1:22267"

            // 2. 创建调用系统浏览器的 Intent
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            // 3. 启动浏览器
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // 这里可以加个 Toast 提示用户没有安装浏览器，但通常不需要
        } finally {
            // 4. 无论成功与否，立即关闭当前 App
            finish()

            // 彻底杀死进程（可选，finish() 通常就够了，但如果你想杀得更干净）
            exitProcess(0)
        }
    }
}
