package com.antago30.bttherm20

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var textViewData: TextView
    private lateinit var imRxStatus: ImageView
    private lateinit var bluetoothService: BluetoothService
    private val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        textViewData = findViewById(R.id.textViewData)
        imRxStatus = findViewById(R.id.imRxStatus)
        bluetoothService = BluetoothService(this)

        checkPermissions()

        bluetoothService.setDataListener(textViewData) { data ->
            runOnUiThread {
                imRxStatus.visibility = View.VISIBLE
                blinkImageView(imRxStatus)
                textViewData.text = data
            }
        }

        // Инициализация Bluetooth
        if (!bluetoothService.isBluetoothEnabled()) {
            Toast.makeText(this, "Включите Bluetooth", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_device -> {
                DeviceChooserDialog(this, bluetoothService).show()
                return true
            }
            R.id.action_reconnect -> {
                bluetoothService.reconnect(bluetoothService)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun blinkImageView(imageView: ImageView) {
        val colorFrom = Color.RED
        val colorTo = Color.GREEN
        val duration = 500L // время мигания в миллисекундах

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo, colorFrom)
        colorAnimation.duration = duration
        colorAnimation.addUpdateListener { animator ->
            imageView.setColorFilter(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
        finish()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_CODE_BLUETOOTH_PERMISSIONS
                )
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_BLUETOOTH_PERMISSIONS)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешения не получены", Toast.LENGTH_LONG).show()
            }
        }
    }
}