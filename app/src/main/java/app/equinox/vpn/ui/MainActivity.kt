package app.equinox.vpn.ui

import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import app.equinox.vpn.ui.theme.EquinoxTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) vm.connect()
    }
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importUri)
    }
    private val scanQr = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { vm.importText("server", it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            EquinoxTheme {
                HomeScreen(
                    vm = vm,
                    onConnect = ::requestConnect,
                    onScanQr = {
                        scanQr.launch(
                            ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt("Scan your WireGuard QR code")
                                .setBeepEnabled(false)
                                .setOrientationLocked(true),
                        )
                    },
                    onImportFile = { pickFile.launch(arrayOf("*/*")) },
                )
            }
        }
    }

    private fun requestConnect() {
        val intent = VpnService.prepare(this)
        if (intent == null) vm.connect() else vpnPermission.launch(intent)
    }
}
