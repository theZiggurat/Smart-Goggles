package c2020.smartgoggles

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var bt = BluetoothSPP(this)
        if(!bt.isBluetoothAvailable){
            Toast.makeText(this, "No Bluetooth availible", Toast.LENGTH_SHORT);
        }


    }
}
