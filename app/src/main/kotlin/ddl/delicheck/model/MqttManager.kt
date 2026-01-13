package ddl.delicheck.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager(
    private val serverUri: String,
    private val deviceId: String,
    private val mqttUsername: String? = null,
    private val mqttPassword: String? = null,
    private val onMessageReceived: (String, String) -> Unit,
    private val onConnectionLost: (String) -> Unit
) {
    private var client: MqttClient? = null

    suspend fun connect(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 用内存持久化避免权限问题
                client = MqttClient(serverUri, deviceId, MemoryPersistence())
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    if (!mqttUsername.isNullOrEmpty()) {
                        userName = mqttUsername
                    }
                    if (!mqttPassword.isNullOrEmpty()) {
                        password = mqttPassword.toCharArray()
                    }
                    connectionTimeout = 10
                    keepAliveInterval = 5
                }
                client?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        onConnectionLost(cause?.message ?: "未知断开")
                    }
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        onMessageReceived(topic ?: "", String(message?.payload ?: ByteArray(0)))
                    }
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })
                client?.connect(options)
                client?.isConnected == true
            } catch (e: Exception) {
            Log.e("MqttManager", "Error", e)
                false
            }
        }
    }

    fun disconnect() {
        try {
            if (client?.isConnected == true) {
                client?.disconnect()
            }
            client?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun subscribe(topic: String): Boolean {
        return withContext(Dispatchers.IO) {
            try { client?.subscribe(topic, 1); true } catch (_: Exception) { false }
        }
    }

    suspend fun publish(topic: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (client?.isConnected != true) return@withContext false
                val msg = MqttMessage(message.toByteArray())
                msg.qos = 1
                client?.publish(topic, msg)
                true
            } catch (_: Exception) { false }
        }
    }

    fun isConnected() = client?.isConnected == true
}
