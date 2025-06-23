package com.antago30.bttherm20

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog

class DeviceChooserDialog(
    private val context: Context,
    private val bluetoothService: BluetoothService
) {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun show() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val devices = adapter?.bondedDevices

        if (devices.isNullOrEmpty()) {
            Toast.makeText(context, "Нет парных устройств", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = devices.map { it.name }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Выберите устройство")
            .setItems(deviceNames) { _, which ->
                devices.elementAtOrNull(which)?.let {
                    Toast.makeText(context, "Подключение к ${it.name}", Toast.LENGTH_SHORT).show()
                    bluetoothService.disconnect()
                    bluetoothService.connectToDevice(it)
                    bluetoothService.setLastDevice(it.address)
                }
            }
            .show()
    }
}