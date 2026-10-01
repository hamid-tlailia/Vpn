package app.equinox.vpn

import android.app.Application
import app.equinox.vpn.data.ProfileStore
import app.equinox.vpn.vpn.VpnController

class EquinoxApp : Application() {
    lateinit var profiles: ProfileStore
        private set
    lateinit var vpn: VpnController
        private set

    override fun onCreate() {
        super.onCreate()
        profiles = ProfileStore(this)
        vpn = VpnController(this)
    }
}
