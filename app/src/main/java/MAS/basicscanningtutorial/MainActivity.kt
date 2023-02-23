package MAS.basicscanningtutorial

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKManager.EMDKListener
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.symbol.emdk.barcode.ScanDataCollection.ScanData
import com.symbol.emdk.barcode.Scanner.*
import com.symbol.emdk.barcode.StatusData.ScannerStates


class MainActivity : AppCompatActivity(), EMDKListener, StatusListener, DataListener {
    private var emdkManager: EMDKManager? = null
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null
    private var dataLength = 0


    // Variables to hold handlers of UI controls
    private var statusTextView: TextView? = null
    private var dataView: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.textViewStatus)
        dataView = findViewById(R.id.editText1)

        val results = EMDKManager.getEMDKManager(applicationContext, this@MainActivity)

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!")
            return
        } else {
            updateStatus("EMDKManager object initialization is   in   progress.......")
        }


    }

    private fun updateData(result: String) {
        this.runOnUiThread{
            if (dataLength++ >=   50) {
                // Clear the cache after 50 scans
                dataView!!.getText().clear();
                dataLength =  0;
            }
            dataView!!.append(result + "\n");
        }
    }

    override fun onOpened(p0: EMDKManager?) {

       emdkManager = p0

        initBarcodeManager()
        initScanner()
    }

    override fun onClosed() {
        if (emdkManager != null) {
            emdkManager!!.release();
            emdkManager = null;
        }
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
    }

    override fun onStatus(p0: StatusData?) {
        val state: ScannerStates = p0!!.getState()
        var statusStr = ""

        when(state){
            ScannerStates.IDLE->{
                // Scanner is idle and ready to change configuration and submit read.
                // Scanner is idle and ready to change configuration and submit read.
                statusStr = p0.getFriendlyName() + " is   enabled and idle..."
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                setConfig()
                try {
                    // Starts an asynchronous Scan. The method will NOT turn ON the scanner beam,
                    //but puts it in a  state in which the scanner can be turned on automatically or by pressing a hardware trigger.
                    scanner!!.read()
                } catch (e: ScannerException) {
                    updateStatus(e.message!!)
                }
            }

            ScannerStates.WAITING->{
                statusStr = "Scanner is waiting for trigger press..."
            }

            ScannerStates.SCANNING->{
                statusStr = "Scanning..."
            }

            ScannerStates.DISABLED->{

            }

            ScannerStates.ERROR->{
                statusStr = "An error has occurred."
            }
            else -> {}
        }
        updateStatus(statusStr);
    }

    private fun updateStatus(status: String) {
        this.runCatching {
            statusTextView!!.setText(""+  status);
        }
    }

    override fun onData(p0: ScanDataCollection?) {
        var dataStr = ""
        if (p0 != null && p0.getResult() === ScannerResults.SUCCESS) {
            val scanData: ArrayList<ScanData> = p0.getScanData()
            // Iterate through scanned data and prepare the data.
            for (data in scanData) {
                // Get the scanned data
                val barcodeData = data.data
                // Get the type of label being scanned
                val labelType = data.labelType
                // Concatenate barcode data and label type
                dataStr = "$barcodeData  $labelType"
            }
            // Update EditText with scanned data and type of label on UI thread.
            updateData(dataStr)
        }
    }

    private fun initBarcodeManager() {
        // Get the feature object such as BarcodeManager object for accessing the feature.
        barcodeManager =
            emdkManager!!.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager?
        // Add external scanner connection listener.
        if (barcodeManager == null) {
            Toast.makeText(this, "Barcode scanning is not supported.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initScanner() {
        if (scanner == null) {
            // Get default scanner defined on the device
            scanner = barcodeManager!!.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            if (scanner != null) {
                // Implement the DataListener interface and pass the pointer of this object to get the data callbacks.
                scanner!!.addDataListener(this)

                // Implement the StatusListener interface and pass the pointer of this object to get the status callbacks.
                scanner!!.addStatusListener(this)

                // Hard trigger. When this mode is set, the user has to manually
                // press the trigger on the device after issuing the read call.
                // NOTE: For devices without a hard trigger, use TriggerType.SOFT_ALWAYS.
                scanner!!.triggerType = TriggerType.HARD
                try {
                    // Enable the scanner
                    // NOTE: After calling enable(), wait for IDLE status before calling other scanner APIs
                    // such as setConfig() or read().
                    scanner!!.enable()
                } catch (e: ScannerException) {
                    updateStatus(e.message!!)
                    deInitScanner()
                }
            } else {
                updateStatus("Failed to   initialize the scanner device.")
            }
        }
    }

    private fun deInitScanner() {
        if (scanner != null) {
            try {
                // Release the scanner
                scanner!!.release()
            } catch (e: Exception) {
                updateStatus(e.message!!)
            }
            scanner = null
        }
    }

    private fun setConfig() {
        if (scanner != null) {
            try {
                // Get scanner config
                val config = scanner!!.config
                // Enable haptic feedback
                if (config.isParamSupported("config.scanParams.decodeHapticFeedback")) {
                    config.scanParams.decodeHapticFeedback = true
                }
                // Set scanner config
                scanner!!.config = config
            } catch (e: ScannerException) {
                updateStatus(e.message!!)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release all the EMDK resources
        if (emdkManager != null) {
            emdkManager!!.release()
            emdkManager = null
        }
    }


}