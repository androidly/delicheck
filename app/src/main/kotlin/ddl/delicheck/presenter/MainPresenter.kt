package ddl.delicheck.presenter

import ddl.delicheck.model.*
import ddl.delicheck.view.MainContract
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainPresenter(
    private val view: MainContract.View,
    private val prefManager: PreferenceManager
) : MainContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mqttManager: MqttManager? = null
    private var appConfig = AppConfig()
    private var userList = ArrayList<User>()
    
    private var customTimeMillis: Long? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeFormatLog = SimpleDateFormat("HH:mm:ss", Locale.getDefault())


    override fun start() {
        appConfig = prefManager.loadConfig()
        loadUsersForCurrentDevice()
    }

    private fun loadUsersForCurrentDevice() {
        userList = prefManager.loadUsers(appConfig.currentDeviceId)
        view.updateUserList(userList)
    }
    
    override fun toggleConnection() {
        scope.launch {
            if (mqttManager?.isConnected() == true) {
                // 断开连接通常很快，可以直接断
                mqttManager?.disconnect()
                view.addLog("已断开连接")
                view.updateConnectionState(false)
            } else {
                view.showConnectingLoading() 
                connect()
            }
        }
    }


    private suspend fun connect() {
        val uri = appConfig.serverUri.replace(" ", "").trim()
        val devId = appConfig.currentDeviceId.replace(" ", "").trim()
        
        if (uri.isEmpty() || devId.isEmpty()) {
            view.showToast("配置缺失")
            view.updateConnectionState(false)
            view.showAdvancedSettings(appConfig)
            return
        }
        view.addLog("正在连接...", "$uri\nID: $devId")
        mqttManager = MqttManager(
            serverUri = uri,
            deviceId = devId,
            mqttUsername = appConfig.mqttUsername,
            mqttPassword = appConfig.mqttPassword,
            onMessageReceived = { topic, msg -> scope.launch { view.addLog("收到消息", "Topic: $topic\nPayload:\n$msg") } },
            onConnectionLost = { reason -> scope.launch { view.addLog("连接断开", reason); view.updateConnectionState(false) } }
        )
        if (mqttManager?.connect() == true) {
            view.addLog("连接成功")
            view.updateConnectionState(true)
            mqttManager?.subscribe("client/$devId")
        } else {
            view.addLog("连接失败", "检查网络或配置")
            view.updateConnectionState(false)
        }
    }

    override fun simulateActiveCheckIn(selectedUser: User?) {
        if (selectedUser == null) { view.showToast("请先选择用户"); return }
        scope.launch {
            val finalTime = customTimeMillis ?: System.currentTimeMillis()
            val finalSec = finalTime / 1000L
            val logTime = dateFormat.format(Date(finalTime))
            val mid = System.currentTimeMillis().toString()
            val json = """{"action":300,"data":{"cmd":"checkin","payload":{"users":[{"check_time":$finalSec,"check_type":"fa","user_id":"${selectedUser.userId}"}]}},"from":"${appConfig.currentDeviceId}","mid":"$mid","time":$finalSec,"to":"${appConfig.toSystemId}"}"""
            
            // 日志：打卡动作
            view.addLog("模拟打卡: ${selectedUser.name}", "时间: $logTime\n数据: $json")
            
            if (mqttManager?.publish(appConfig.topicReport, json) == true) view.addLog("发送成功") else view.addLog("发送失败")
        }
    }
    
    override fun simulateRemoteCheckIn() {
        scope.launch {
            val topic = "client/${appConfig.currentDeviceId}"
            val json = """{"mid":"test_${System.currentTimeMillis()}","action":301,"data":{"cmd":"checkin"}}"""
            view.addLog("模拟远程指令", topic)
            mqttManager?.publish(topic, json)
        }
    }

    override fun addUser(name: String, id: String) {
        if (name.isBlank() || id.isBlank()) return
        userList.add(User(name, id))
        prefManager.saveUsers(appConfig.currentDeviceId, userList)
        view.updateUserList(userList)
        view.showToast("已添加")
        
        view.addLog("添加用户", "姓名: $name\nID: $id")
    }

    override fun deleteUser(user: User) {
        userList.remove(user)
        prefManager.saveUsers(appConfig.currentDeviceId, userList)
        view.updateUserList(userList)
        view.showToast("已删除")
        
        view.addLog("删除用户", "已移除: ${user.name}")
    }

    override fun setCustomTime(timestamp: Long?) {
        customTimeMillis = timestamp
        if (timestamp == null) {
            view.updateTimeDisplay("当前时间 (自动)", null)
            view.hideSmartTimeSelection()
            view.addLog("重置时间", "恢复自动模式")
        } else {
            val str = dateFormat.format(Date(timestamp))
            view.updateTimeDisplay(str, null)
            view.addLog("时间已修改", "设定为: $str")
        }
    }

    override fun generateSmartTime() {
        try {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val isSummer = currentMonth in 5..9
            
            val todayPrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val amStartTs = fmt.parse("$todayPrefix ${appConfig.amStart}")!!.time
            val amEndTs = fmt.parse("$todayPrefix ${appConfig.amEnd}")!!.time
            val amRandom = generateRandomInRange(amStartTs, amEndTs)

            val pmStartStr = if (isSummer) appConfig.pmSummerStart else appConfig.pmWinterStart
            val pmEndStr = if (isSummer) appConfig.pmSummerEnd else appConfig.pmWinterEnd
            val pmStartTs = fmt.parse("$todayPrefix $pmStartStr")!!.time
            val pmEndTs = fmt.parse("$todayPrefix $pmEndStr")!!.time
            val pmRandom = generateRandomInRange(pmStartTs, pmEndTs)

            val now = System.currentTimeMillis()
            val distAm = abs(now - amRandom)
            val distPm = abs(now - pmRandom)
            
            view.showSmartTimeSelection(amRandom, pmRandom, distAm <= distPm)
            view.showToast("生成成功，请选择时段")
            
            val amStr = timeFormatLog.format(Date(amRandom))
            val pmStr = timeFormatLog.format(Date(pmRandom))
            view.addLog("智能推荐生成", "上午候选: $amStr\n下午候选: $pmStr")
            
        } catch (e: Exception) {
            view.showToast("生成失败: ${e.message}")
            view.addLog("智能推荐出错", e.message)
        }
    }
    
    private fun generateRandomInRange(start: Long, end: Long): Long {
        if (end <= start) return start
        val diff = Random().nextInt((end - start).toInt())
        val sec = Random().nextInt(60) * 1000L
        var res = start + diff + sec
        if (res > end) res = end
        return res
    }

    override fun openSettings() {
        view.showAdvancedSettings(appConfig)
    }

    override fun saveSettings(newConfig: AppConfig) {
        val oldDeviceId = appConfig.currentDeviceId
        appConfig = newConfig
        prefManager.saveConfig(appConfig)
        view.showToast("设置已保存")
        view.addLog("配置更新", "设置已保存")
        
        if (oldDeviceId != newConfig.currentDeviceId) {
            view.addLog("切换设备", "ID: ${newConfig.currentDeviceId}")
            loadUsersForCurrentDevice()
            scope.launch { 
                if (mqttManager?.isConnected() == true) {
                    mqttManager?.disconnect() 
                    view.updateConnectionState(false)
                }
            }
        }
    }
    
    override fun requestExport() {
        val json = prefManager.exportData()
        view.showExportDialog(json)
        view.addLog("数据导出", "已生成备份代码")
    }

    override fun performImport(json: String) {
        if (json.isBlank()) return
        if (prefManager.importData(json)) {
            view.showToast("导入成功")
            view.addLog("数据导入", "配置已覆盖，正在重启逻辑...")
            start()
        } else {
            view.showToast("格式错误")
            view.addLog("导入失败", "JSON解析错误")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        mqttManager?.disconnect()
    }

    override fun isConfigValid(): Boolean {
        return appConfig.serverUri.isNotBlank() && appConfig.currentDeviceId.isNotBlank()
    }

    override fun hasUsers(): Boolean {
        return userList.isNotEmpty()
    }

    override fun isConnected(): Boolean {
        return mqttManager?.isConnected() == true
    }
}
