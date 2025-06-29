package com.antago30.bttherm20

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission

import java.io.IOException
import java.util.UUID

class BluetoothService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var dataListener: ((String) -> Unit)? = null
    private var listenThread: Thread? = null
    private lateinit var textViewData: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var lastDevice: BluetoothDevice
    private var isReconnecting = false

    fun isBluetoothEnabled() = bluetoothAdapter.isEnabled

    fun setDataListener(tvText: TextView,listener: (String) -> Unit) {
        textViewData = tvText
        dataListener = listener
    }

    fun setLastDevice(macAddress: String?) {
        lastDevice = bluetoothAdapter.getRemoteDevice(macAddress)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun reconnect() {
        disconnect()
        try {
            connectToDevice(lastDevice)
            handler.post {
                Toast.makeText(context, "Повторное подключение", Toast.LENGTH_LONG).show()
            }
        } catch (i: UninitializedPropertyAccessException) {
            handler.post {
                Toast.makeText(context, "Для первого подключения выберите устройство из списка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()

        } catch (e: IOException) {
            // Игнор
        }
        bluetoothSocket = null
        listenThread?.interrupt()
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice) {
        isReconnecting = false
        Thread  {
            try {
                val uuid = device.uuids?.firstOrNull()?.uuid
                    ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                bluetoothSocket = socket
                startListening()
            } catch (e: IOException) {
                handler.post {
                    Toast.makeText(context, "Устройство не найдено", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startListening() {
        val inputStream = bluetoothSocket?.inputStream ?: return
        listenThread = Thread {
            val buffer = ByteArray(1024)
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 2){
                        isReconnecting = true
                        val data = String(buffer, 0, bytesRead).replace(Regex("[\n\r^@\\s ]"), "")
                            .replace(Regex("[-–−—]"),"–").trim()
                        dataListener?.invoke(data)
                    }
                } catch (e: IOException) {
                    disconnect()
                    attemptReconnect()
                    break
                }
            }
        }.apply { start() }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun attemptReconnect() {
        handler.post {
            textViewData.text = "Error"
        }

        if (!isReconnecting) return
        isReconnecting = false

        handler.postDelayed({
            Toast.makeText(context, "Попытка повторного соединения", Toast.LENGTH_SHORT).show()
            connectToDevice(lastDevice)
        }, 3000)

    }
}

