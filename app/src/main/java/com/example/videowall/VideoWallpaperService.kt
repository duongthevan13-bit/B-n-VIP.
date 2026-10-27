package com.example.videowall

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var player: MediaPlayer? = null
        private var ready = false
        private var visible = false
        private val prefs by lazy { getSharedPreferences("wp", MODE_PRIVATE) }
        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "rev" && surfaceHolder.surface.isValid) load()
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            release()
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            load()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            release()
            super.onSurfaceDestroyed(holder)
        }

        // Bật màn hình bằng nút nguồn => chạy video từ đầu.
        // Tắt màn hình => dừng. Chạy hết thì giữ khung hình cuối.
        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) playFromStart() else player?.takeIf { ready }?.pause()
        }

        private fun load() {
            release()
            val file = File(filesDir, "wallpaper.mp4")
            if (!file.exists()) return
            player = MediaPlayer().apply {
                setSurface(surfaceHolder.surface)
                setDataSource(file.path)
                isLooping = false
                setVolume(0f, 0f)
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                setOnPreparedListener {
                    ready = true
                    if (visible) playFromStart() else seekTo(0)
                }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
        }

        private fun playFromStart() {
            val p = player ?: return
            if (!ready) return
            p.seekTo(0)
            p.start()
        }

        private fun release() {
            ready = false
            try { player?.release() } catch (_: Exception) {}
            player = null
        }
    }
}
