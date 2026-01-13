package ddl.delicheck.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import ddl.delicheck.R
import ddl.delicheck.model.AppConfig
import java.util.ArrayList

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    // 控件引用
    private lateinit var etServer: EditText
    private lateinit var etMqttUsername: EditText
    private lateinit var etMqttPassword: EditText
    private lateinit var etToSystemId: EditText
    private lateinit var etTopicReport: EditText
    private lateinit var spDevice: Spinner
    private lateinit var etAmStart: EditText
    private lateinit var etAmEnd: EditText
    private lateinit var etSummerStart: EditText
    private lateinit var etSummerEnd: EditText
    private lateinit var etWinterStart: EditText
    private lateinit var etWinterEnd: EditText

    // 本地维护的设备列表，用于 Spinner 展示
    private val deviceList = ArrayList<String>()
    private lateinit var deviceAdapter: ArrayAdapter<String>

    // 暂存当前配置，避免空指针
    private var currentConfig = AppConfig()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 绑定控件
        etServer = view.findViewById(R.id.etSettingsServer)
        etMqttUsername = view.findViewById(R.id.etMqttUsername)
        etMqttPassword = view.findViewById(R.id.etMqttPassword)
        etToSystemId = view.findViewById(R.id.etToSystemId)
        etTopicReport = view.findViewById(R.id.etTopicReport)
        spDevice = view.findViewById(R.id.spDeviceId)
        etAmStart = view.findViewById(R.id.etAmStart)
        etAmEnd = view.findViewById(R.id.etAmEnd)
        etSummerStart = view.findViewById(R.id.etSummerStart)
        etSummerEnd = view.findViewById(R.id.etSummerEnd)
        etWinterStart = view.findViewById(R.id.etWinterStart)
        etWinterEnd = view.findViewById(R.id.etWinterEnd)

        // Spinner适配器
        deviceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, deviceList)
        spDevice.adapter = deviceAdapter

        val act = requireActivity() as MainActivity
        val presenter = act.presenter

        // 保存设置
        view.findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val newConfig = collectConfig()
            presenter.saveSettings(newConfig)
        }

        // 恢复默认
        view.findViewById<Button>(R.id.btnResetDefault).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("确认重置")
                .setMessage("这将恢复默认的服务器地址、MQTT 认证和时间规则，是否继续？")
                .setPositiveButton("重置") { _, _ ->
                    resetUIValues()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 添加设备 ID
        view.findViewById<View>(R.id.btnAddDevice).setOnClickListener {
            val input = EditText(requireContext())
            input.hint = "输入新的设备 ID"
            AlertDialog.Builder(requireContext())
                .setTitle("添加设备")
                .setView(input)
                .setPositiveButton("添加") { _, _ ->
                    val newId = input.text.toString().trim()
                    if (newId.isNotEmpty() && !deviceList.contains(newId)) {
                        deviceList.add(newId)
                        deviceAdapter.notifyDataSetChanged()
                        spDevice.setSelection(deviceList.size - 1) // 选中新加的
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 删除设备 ID
        view.findViewById<View>(R.id.btnDelDevice).setOnClickListener {
            if (deviceList.size <= 1) {
                Toast.makeText(requireContext(), "至少需要保留一个设备 ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedPos = spDevice.selectedItemPosition
            if (selectedPos >= 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("删除设备")
                    .setMessage("确定要删除当前选中的设备 ID 吗？")
                    .setPositiveButton("删除") { _, _ ->
                        deviceList.removeAt(selectedPos)
                        deviceAdapter.notifyDataSetChanged()
                        spDevice.setSelection(0)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val presenter = (requireActivity() as MainActivity).presenter
        presenter.openSettings()
    }

    // 由 MainActivity 调用，用于填充界面数据
    fun displayConfig(config: AppConfig) {
        if (!isAdded) return
        this.currentConfig = config

        // 填充文本框
        etServer.setText(config.serverUri)
        etMqttUsername.setText(config.mqttUsername)
        etMqttPassword.setText(config.mqttPassword)
        etToSystemId.setText(config.toSystemId)
        etTopicReport.setText(config.topicReport)
        etAmStart.setText(config.amStart)
        etAmEnd.setText(config.amEnd)
        etSummerStart.setText(config.pmSummerStart)
        etSummerEnd.setText(config.pmSummerEnd)
        etWinterStart.setText(config.pmWinterStart)
        etWinterEnd.setText(config.pmWinterEnd)

        // 填充设备列表
        deviceList.clear()
        deviceList.addAll(config.savedDeviceIds)
        // 确保列表非空
        if (deviceList.isEmpty() && config.currentDeviceId.isNotBlank()) {
            deviceList.add(config.currentDeviceId)
        }
        deviceAdapter.notifyDataSetChanged()

        // 选中当前设备 ID
        val index = deviceList.indexOf(config.currentDeviceId)
        if (index >= 0) {
            spDevice.setSelection(index)
        }
    }

    // 从 UI 收集数据生成 AppConfig 对象
    private fun collectConfig(): AppConfig {
        // 获取当前选中的 Device ID
        val selectedId = spDevice.selectedItem?.toString() ?: currentConfig.currentDeviceId
        
        // 构造新的配置对象
        return currentConfig.copy(
            serverUri = etServer.text.toString().trim(),
            mqttUsername = etMqttUsername.text.toString().trim(),
            mqttPassword = etMqttPassword.text.toString().trim(),
            toSystemId = etToSystemId.text.toString().trim(),
            topicReport = etTopicReport.text.toString().trim(),
            currentDeviceId = selectedId,
            savedDeviceIds = ArrayList(deviceList),
            amStart = etAmStart.text.toString().trim(),
            amEnd = etAmEnd.text.toString().trim(),
            pmSummerStart = etSummerStart.text.toString().trim(),
            pmSummerEnd = etSummerEnd.text.toString().trim(),
            pmWinterStart = etWinterStart.text.toString().trim(),
            pmWinterEnd = etWinterEnd.text.toString().trim()
        )
    }

    @SuppressLint("SetTextI18n")
    private fun resetUIValues() {
        etServer.setText("")
        etMqttUsername.setText("")
        etMqttPassword.setText("")
        etToSystemId.setText("")
        etTopicReport.setText("device")
        etAmStart.setText("07:50")
        etAmEnd.setText("08:15")
        etSummerStart.setText("14:45")
        etSummerEnd.setText("15:15")
        etWinterStart.setText("14:15")
        etWinterEnd.setText("14:40")
        Toast.makeText(requireContext(), "界面已重置，请点击底部按钮保存", Toast.LENGTH_SHORT).show()
    }
}
