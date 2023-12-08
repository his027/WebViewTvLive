package com.hxh19950701.webviewtvlive.adapter

import android.view.KeyEvent
import com.hxh19950701.webviewtvlive.widget.WebpageAdapterWebView

class DwWebpageAdapter:CommonWebpageAdapter() {

    override fun isAdaptedUrl(url: String) = url.contains("dw.com")

    override fun userAgent() = null

    override fun javascript(): String {
        return """
            var btn = document.getElementsByClassName("vjs-big-play-button")[0];
            if (btn) {
                btn.click();
            }
        """.trimIndent() + super.javascript()
    }

    override suspend fun enterFullscreen(webView: WebpageAdapterWebView) {
        enterFullscreenByPressKey(webView, KeyEvent.KEYCODE_F)
    }

}