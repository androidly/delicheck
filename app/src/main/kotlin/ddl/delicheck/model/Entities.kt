package ddl.delicheck.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_name_in_json") val name: String,
    @SerializedName("user_id_in_json") val userId: String
) {
    override fun toString() = name
}

data class LogItem(
    val time: String,
    val summary: String,
    val detail: String? = null,
    var isExpanded: Boolean = false
)

data class AppConfig(
    // 连接配置
    var serverUri: String = "",
    var currentDeviceId: String = "",
    
    // MQTT认证
    var mqttUsername: String = "",
    var mqttPassword: String = "",
    
    // 消息发送配置
    var toSystemId: String = "",
    var topicReport: String = "device",

    // 设备历史记录
    var savedDeviceIds: MutableList<String> = mutableListOf(),

    // 时间规则
    var amStart: String = "07:50", var amEnd: String = "08:15",
    var pmSummerStart: String = "14:45", var pmSummerEnd: String = "15:15",
    var pmWinterStart: String = "14:15", var pmWinterEnd: String = "14:40"
)

data class BackupData(
    val config: AppConfig,
    val users: Map<String, List<User>> 
)
