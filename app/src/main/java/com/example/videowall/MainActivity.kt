package com.example.videowall

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) saveVideo(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            text = if (File(filesDir, "wallpaper.mp4").exists()) "Đã có video. Bấm \"Đặt làm hình nền\"."
                   else "Chưa chọn video."
        }
        val pick = Button(this).apply {
            text = "Chọn video"
            setOnClickListener { pickVideo.launch("video/*") }
        }
        val set = Button(this).apply {
            text = "Đặt làm hình nền"
            setOnClickListener {
                val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                )
                startActivity(i)
            }
        }
        root.addView(status); root.addView(pick); root.addView(set)
        setContentView(root)
    }

    private fun saveVideo(uri: Uri) {
        status.text = "Đang lưu video..."
        thread {
            try {
                contentResolver.openInputStream(uri)!!.use { input ->
                    File(filesDir, "wallpaper.mp4").outputStream().use { input.copyTo(it) }
                }
                getSharedPreferences("wp", MODE_PRIVATE).edit()
                    .putLong("rev", System.currentTimeMillis()).apply()
                runOnUiThread { status.text = "Đã lưu video. Bấm \"Đặt làm hình nền\"." }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Không đọc được video", Toast.LENGTH_LONG).show() }
            }
        }
    }
}
