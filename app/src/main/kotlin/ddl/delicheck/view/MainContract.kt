package ddl.delicheck.view

import ddl.delicheck.model.AppConfig
import ddl.delicheck.model.User

interface MainContract {
    interface View {
        fun showToast(msg: String)
        fun addLog(summary: String, detail: String? = null)
        fun updateConnectionState(isConnected: Boolean)
        fun updateUserList(users: List<User>)
        fun updateTimeDisplay(timeStr: String, ruleTip: String? = null)
        
        fun showSmartTimeSelection(amTime: Long, pmTime: Long, defaultSelectionIsAm: Boolean)
        fun hideSmartTimeSelection()

        fun showAdvancedSettings(config: AppConfig)
        fun showExportDialog(json: String)
        fun showImportDialog()
        fun showConnectingLoading() 
    }

    interface Presenter {
        fun start()
        fun toggleConnection()
        fun simulateActiveCheckIn(selectedUser: User?)
        fun simulateRemoteCheckIn()
        fun addUser(name: String, id: String)
        fun deleteUser(user: User)
        
        fun setCustomTime(timestamp: Long?)
        fun generateSmartTime()
        
        fun openSettings()
        fun saveSettings(newConfig: AppConfig)
        
        fun requestExport()
        fun performImport(json: String)
        fun onDestroy()
        
        fun isConfigValid(): Boolean
        fun hasUsers(): Boolean
        fun isConnected(): Boolean
    }
}
