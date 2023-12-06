package com.hxh19950701.webviewtvlive

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hxh19950701.webviewtvlive.playlist.Playlist.Companion.firstChannel
import com.hxh19950701.webviewtvlive.playlist.PlaylistManager
import com.hxh19950701.webviewtvlive.widget.ChannelPlayerView
import com.hxh19950701.webviewtvlive.widget.ExitConfirmView
import com.hxh19950701.webviewtvlive.widget.PlaylistView
import com.hxh19950701.webviewtvlive.widget.SettingsView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        const val LAST_CHANNEL = "last_channel"
    }

    enum class UiMode {
        STANDARD, CHANNELS, EXIT_CONFIRM, SETTINGS
    }

    private lateinit var playerView: ChannelPlayerView
    private lateinit var mainLayout: FrameLayout
    private lateinit var playlistView: PlaylistView
    private lateinit var loadingPlaylistView: TextView
    private lateinit var exitConfirmView: ExitConfirmView
    private lateinit var settingsView: SettingsView
    private var isDestroyed = false

    private var uiMode = UiMode.STANDARD
        set(value) {
            if (field == value) return
            field = value
            playlistView.visibility = if (value == UiMode.CHANNELS) View.VISIBLE else View.GONE
            exitConfirmView.visibility = if (value == UiMode.EXIT_CONFIRM) View.VISIBLE else View.GONE
            settingsView.visibility = if (value == UiMode.SETTINGS) View.VISIBLE else View.GONE
        }

    private val backToStandardModeAction = Runnable { uiMode = UiMode.STANDARD }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainLayout = findViewById(R.id.mainLayout)
        playerView = findViewById(R.id.player)
        playlistView = findViewById(R.id.playlist)
        loadingPlaylistView = findViewById(R.id.loadingPlaylist)
        exitConfirmView = findViewById(R.id.exitConfirm)
        settingsView = findViewById(R.id.settings)

        playlistView.onChannelSelectCallback = {
            preference.edit().putString(LAST_CHANNEL, it.name).apply()
            playerView.channel = it
            playlistView.post { uiMode = UiMode.STANDARD }
        }
        exitConfirmView.onUserSelection = {
            if (it == ExitConfirmView.Selection.EXIT) finish() else uiMode = UiMode.SETTINGS
        }
        playerView.activity = this
        playerView.dismissAllViewCallback = { uiMode = UiMode.STANDARD }
        playerView.setOnClickListener { uiMode = if (uiMode == UiMode.STANDARD) UiMode.CHANNELS else UiMode.STANDARD }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        CoroutineScope(Dispatchers.Main).launch {
            val playlist = withContext(Dispatchers.IO) { PlaylistManager.loadPlaylist() }
            if (!isDestroyed) {
                playlistView.playlist = playlist
                loadingPlaylistView.visibility = View.GONE
                autoPlay()
            }
        }
    }

    private fun autoPlay() {
        if (playlistView.playlist != null) {
            val lastChannelName = preference.getString(LAST_CHANNEL, null)
            val lastChannel = playlistView.playlist!!.getAllChannels().firstOrNull { it.name == lastChannelName }
            if (lastChannel != null) {
                playlistView.currentChannel = lastChannel
            } else {
                playlistView.playlist.firstChannel()?.let { playlistView.currentChannel = it }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        uiMode = UiMode.STANDARD
    }

    override fun onDestroy() {
        isDestroyed = true
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        uiMode = if (uiMode == UiMode.STANDARD) UiMode.EXIT_CONFIRM else UiMode.STANDARD
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        repostBackToStandardModeAction()
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        repostBackToStandardModeAction()
        when (uiMode) {
            UiMode.CHANNELS -> if (playlistView.dispatchKeyEvent(event)) return true
            UiMode.EXIT_CONFIRM -> if (exitConfirmView.dispatchKeyEvent(event)) return true
            UiMode.SETTINGS -> if (settingsView.dispatchKeyEvent(event)) return true
            else -> if(event.action == KeyEvent.ACTION_UP) onKeyUp(event.keyCode, event)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (uiMode) {
            UiMode.STANDARD -> {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> playlistView.previousChannel()
                    KeyEvent.KEYCODE_DPAD_DOWN -> playlistView.nextChannel()
                    KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_ENTER -> uiMode = UiMode.CHANNELS
                }
            }

            else -> {}
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun repostBackToStandardModeAction() {
        playerView.removeCallbacks(backToStandardModeAction)
        playerView.postDelayed(backToStandardModeAction, 5000L)
    }
}