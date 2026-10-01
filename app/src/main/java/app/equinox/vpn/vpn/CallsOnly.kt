package app.equinox.vpn.vpn

import android.content.pm.PackageManager
import com.wireguard.config.Config
import com.wireguard.config.Interface

/** "Calls only" mode: route just WhatsApp and Messenger through the tunnel. */
object CallsOnly {
    val PACKAGES = listOf(
        "com.whatsapp",          // WhatsApp
        "com.whatsapp.w4b",      // WhatsApp Business
        "com.facebook.orca",     // Messenger
        "com.facebook.mlite",    // Messenger Lite
    )

    fun installed(pm: PackageManager): List<String> = PACKAGES.filter {
        runCatching { pm.getPackageInfo(it, 0) }.isSuccess
    }

    /** Returns a copy of [config] that only tunnels [apps]. */
    fun restrict(config: Config, apps: Collection<String>): Config {
        val src = config.`interface`
        val iface = Interface.Builder()
            .setKeyPair(src.keyPair)
            .addAddresses(src.addresses)
            .addDnsServers(src.dnsServers)
            .addDnsSearchDomains(src.dnsSearchDomains)
            .includeApplications(apps)
            .apply {
                src.listenPort.ifPresent { setListenPort(it) }
                src.mtu.ifPresent { setMtu(it) }
            }
            .build()
        return Config.Builder().setInterface(iface).addPeers(config.peers).build()
    }
}
