package app.equinox.vpn.data

import android.os.Build
import com.wireguard.crypto.KeyPair
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Creates a free Cloudflare WARP device and returns a ready-to-use wg-quick config.
 * Mirrors what the open-source `wgcf` tool does, so every install gets its own account.
 */
object WarpRegistrar {
    private const val API = "https://api.cloudflareclient.com/v0a5641/reg"
    private const val USER_AGENT = "1.1.1.1/6.38.9-5641 (Android 16.0.0)"
    private const val CLIENT_VERSION = "a-6.38.9-5641"

    /** Blocking — call from a background dispatcher. */
    fun register(): String {
        val keys = KeyPair()
        val body = JSONObject()
            .put("fcm_token", "")
            .put("install_id", "")
            .put("key", keys.publicKey.toBase64())
            .put("locale", "en_US")
            .put("model", Build.MODEL ?: "Android")
            .put("tos", Instant.now().toString())
            .put("serial_number", "")
            .put("os_version", "16.0.0")
            .put("key_type", "curve25519")
            .put("tunnel_type", "wireguard")

        val conn = (URL(API).openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = WarpTls(sslSocketFactory)
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("CF-Client-Version", CLIENT_VERSION)
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Connection", "Keep-Alive")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Cloudflare returned HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.use { it.reader().readText() })
            return buildConfig(keys, json.getJSONObject("config"))
        } finally {
            conn.disconnect()
        }
    }

    private fun buildConfig(keys: KeyPair, config: JSONObject): String {
        val addresses = config.getJSONObject("interface").getJSONObject("addresses")
        val peer = config.getJSONArray("peers").getJSONObject(0)
        return """
            [Interface]
            PrivateKey = ${keys.privateKey.toBase64()}
            Address = ${addresses.getString("v4")}/32, ${addresses.getString("v6")}/128
            DNS = 1.1.1.1, 1.0.0.1, 2606:4700:4700::1111, 2606:4700:4700::1001
            MTU = 1280

            [Peer]
            PublicKey = ${peer.getString("public_key")}
            AllowedIPs = 0.0.0.0/0, ::/0
            Endpoint = ${endpoint(peer.getJSONObject("endpoint"))}
            PersistentKeepalive = 25
        """.trimIndent()
    }

    private fun endpoint(e: JSONObject): String {
        val port = e.optJSONArray("ports")?.optInt(0, 2408)?.takeIf { it > 0 } ?: 2408
        val host = e.optString("host").ifEmpty { e.optString("v4") }.ifEmpty { e.optString("v6") }
        if (host.isEmpty()) throw IOException("Cloudflare response had no endpoint")
        // Strip any port the API attached, then use ours.
        val bare = when {
            host.startsWith("[") -> host.substringAfter("[").substringBefore("]")
            host.count { it == ':' } == 1 -> host.substringBefore(":")
            else -> host
        }
        return if (':' in bare) "[$bare]:$port" else "$bare:$port"
    }

    /** TLS 1.2 with the same cipher suites as the official Android client. */
    private class WarpTls(private val base: SSLSocketFactory) : SSLSocketFactory() {
        private val suites = arrayOf(
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        )

        private fun tune(socket: Socket): Socket = socket.apply {
            if (this is SSLSocket) {
                if ("TLSv1.2" in supportedProtocols) enabledProtocols = arrayOf("TLSv1.2")
                val usable = suites.filter { it in supportedCipherSuites }
                if (usable.isNotEmpty()) enabledCipherSuites = usable.toTypedArray()
            }
        }

        override fun getDefaultCipherSuites(): Array<String> = base.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = base.supportedCipherSuites
        override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean) = tune(base.createSocket(s, host, port, autoClose))
        override fun createSocket(host: String, port: Int) = tune(base.createSocket(host, port))
        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int) = tune(base.createSocket(host, port, localHost, localPort))
        override fun createSocket(host: InetAddress, port: Int) = tune(base.createSocket(host, port))
        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int) = tune(base.createSocket(address, port, localAddress, localPort))
    }
}
