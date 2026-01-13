package ddl.delicheck.model

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val keyConfig = "AppConfig"
    
    // 旧数据key，用于迁移
    private val keyLegacyUsers = "Users"

    fun loadConfig(): AppConfig {
        val json = prefs.getString(keyConfig, null)
        val config = if (json != null) {
            try { gson.fromJson(json, AppConfig::class.java) } catch (_: Exception) { AppConfig() }
        } else {
            AppConfig()
        }
        
        // 如果当前设备ID不在列表中则补上
        if (config.currentDeviceId.isNotEmpty() && !config.savedDeviceIds.contains(config.currentDeviceId)) {
            config.savedDeviceIds.add(config.currentDeviceId)
        }
        return config
    }

    fun saveConfig(config: AppConfig) {
        // 确保ID在列表中
        if (config.currentDeviceId.isNotEmpty() && !config.savedDeviceIds.contains(config.currentDeviceId)) {
            config.savedDeviceIds.add(config.currentDeviceId)
        }
        prefs.edit { putString(keyConfig, gson.toJson(config)) }
    }

    fun loadUsers(deviceId: String): ArrayList<User> {
        if (deviceId.isEmpty()) return ArrayList()
        
        val targetKey = "Users_$deviceId"
        val json = prefs.getString(targetKey, null)
        val list = ArrayList<User>()

        if (json != null) {
            try { list.addAll(gson.fromJson(json, object : TypeToken<List<User>>() {}.type)) } catch (_: Exception) { }
        } else {
            // 尝试从旧key迁移数据
            val legacyJson = prefs.getString(keyLegacyUsers, null)
            if (legacyJson != null) {
                try {
                    val legacyList: List<User> = gson.fromJson(legacyJson, object : TypeToken<List<User>>() {}.type)
                    if (legacyList.isNotEmpty()) {
                        list.addAll(legacyList)
                        saveUsers(deviceId, list)
                    }
                } catch (_: Exception) {}
            }
        }
        
        return list
    }

    fun saveUsers(deviceId: String, users: List<User>) {
        if (deviceId.isEmpty()) return
        val key = "Users_$deviceId"
        prefs.edit { putString(key, gson.toJson(users)) }
    }

    fun exportData(): String {
        val config = loadConfig()
        val usersMap = HashMap<String, List<User>>()
        for (id in config.savedDeviceIds) {
            usersMap[id] = loadUsers(id)
        }
        return gson.toJson(BackupData(config, usersMap))
    }

    fun importData(json: String): Boolean {
        return try {
            val backup = gson.fromJson(json, BackupData::class.java)
            if (backup != null) {
                saveConfig(backup.config)
                backup.users.forEach { (deviceId, uList) ->
                    saveUsers(deviceId, uList)
                }
                true
            } else false
        } catch (e: Exception) {
            Log.e("PreferenceManager", "Import error", e)
            false
        }
    }
}
