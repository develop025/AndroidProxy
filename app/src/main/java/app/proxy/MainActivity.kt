package app.proxy

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executors

const val TAG = "Proxy"

class MainActivity : AppCompatActivity() {

    private lateinit var wv: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wv = findViewById(R.id.webView)

        setProxy("103.152.5.70", 8080)

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        wv.webViewClient = WebViewClientCookieSupport(this)

//        wv.loadUrl("https://www.myip.com")
    }

    @SuppressLint("PrivateApi")
    private fun setProxy(host: String, port: Int) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyUrl = "${host}:${port}"
            val proxyConfig: ProxyConfig = ProxyConfig.Builder()
                    .addProxyRule(proxyUrl)
                    //.addDirect()//when proxy is not working, use direct connect, maybe?
                    .build()
            ProxyController.getInstance().setProxyOverride(proxyConfig,
                    Executors.newCachedThreadPool(),
                    {
                        Log.d(TAG, "WebView listener")
                        this.runOnUiThread {
                            wv.loadUrl("https://www.myip.com")
                        }
                    })
        } else {
            // use the solution of other anwsers
            Log.d(TAG, "PROXY_OVERRIDE not supported")

//            System.getProperties().put("proxySet", "true")
//            System.setProperty("http.proxyHost", host)
//            System.setProperty("http.proxyPort", "$port")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ProxyManager.setProxy(applicationContext, host, "$port")
                wv.loadUrl("https://www.myip.com")
            }
        }
    }
}