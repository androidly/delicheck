package ddl.delicheck.view

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import ddl.delicheck.R
import ddl.delicheck.model.LogItem
import ddl.delicheck.model.User
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val logList = ArrayList<LogItem>()
    private lateinit var logAdapter: LogAdapter
    private lateinit var userAdapter: ArrayAdapter<User>
    private val timeFormatOnly = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val timeFormatShort = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 视图引用
    private var rvLog: RecyclerView? = null
    private var cgSmartTime: ChipGroup? = null
    private var tvSelectedTime: TextView? = null
    private var chipRuleTip: Chip? = null
    private var viewStatusDot: View? = null
    private var tvStatus: TextView? = null
    private var btnConnect: Button? = null
    private var btnSimulateActive: Button? = null
    private var chipAm: Chip? = null
    private var chipPm: Chip? = null
    private var spinnerUser: Spinner? = null
    private var btnDeleteUser: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val act = requireActivity() as MainActivity
        val presenter = act.presenter

        // 绑定控件
        rvLog = view.findViewById(R.id.rvLog)
        cgSmartTime = view.findViewById(R.id.cgSmartTime)
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime)
        chipRuleTip = view.findViewById(R.id.chipRuleTip)
        viewStatusDot = view.findViewById(R.id.viewStatusDot)
        tvStatus = view.findViewById(R.id.tvStatus)
        btnConnect = view.findViewById(R.id.btnConnect)
        btnSimulateActive = view.findViewById(R.id.btnSimulateActiveCheckIn)
        chipAm = view.findViewById(R.id.chipAm)
        chipPm = view.findViewById(R.id.chipPm)

        // 初始化列表
        logAdapter = LogAdapter(logList)
        rvLog?.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        rvLog?.adapter = logAdapter

        userAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ArrayList())
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUser = view.findViewById(R.id.spinnerUser)
        spinnerUser?.adapter = userAdapter
        btnDeleteUser = view.findViewById(R.id.btnDeleteUser)

        // 绑定事件
        btnConnect?.setOnClickListener { presenter.toggleConnection() }
        
        btnSimulateActive?.setOnClickListener { 
            presenter.simulateActiveCheckIn(spinnerUser?.selectedItem as? User) 
        }

        // 添加用户
        view.findViewById<View>(R.id.btnAddUser).setOnClickListener {
            val v = LayoutInflater.from(context).inflate(R.layout.dialog_add_user, null)
            AlertDialog.Builder(requireContext())
                .setTitle("添加用户")
                .setView(v)
                .setPositiveButton("添加") { _, _ ->
                    val name = v.findViewById<EditText>(R.id.etUserName).text.toString()
                    val id = v.findViewById<EditText>(R.id.etUserId).text.toString()
                    presenter.addUser(name, id)
                }
                .setNegativeButton("取消", null)
                .show()
        }
        
        // 删除用户
        btnDeleteUser?.setOnClickListener {
            (spinnerUser?.selectedItem as? User)?.let { user ->
                AlertDialog.Builder(requireContext())
                    .setTitle("删除")
                    .setMessage("确认删除 ${user.name}?")
                    .setPositiveButton("是") { _, _ -> 
                        presenter.deleteUser(user) 
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        view.findViewById<View>(R.id.btnSmartTime).setOnClickListener { presenter.generateSmartTime() }
        view.findViewById<View>(R.id.btnPickTime).setOnClickListener { showDateTimePicker(presenter) }
        view.findViewById<View>(R.id.btnResetTime).setOnClickListener { presenter.setCustomTime(null) }

        // 单选Chip的选中事件
        cgSmartTime?.setOnCheckedStateChangeListener { group, checkedIds ->
            // checkedIds 是一个 List<Int>，包含了所有选中的 Chip ID
            if (checkedIds.isNotEmpty()) {
            // 单选模式下取第一个
                val checkedId = checkedIds[0]
                
                val chip = group.findViewById<Chip>(checkedId)
                val timestamp = chip.tag as Long
                presenter.setCustomTime(timestamp)
                cgSmartTime?.visibility = View.VISIBLE
            }
        }

        // 初始化UI状态
        updateConnectionState(false)
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.presenter?.start()
    }

    private fun getThemeColor(attrId: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    // UI 更新方法

    fun addLog(summary: String, detail: String?) {
        if (!isAdded) return
        activity?.runOnUiThread {
            logList.add(LogItem(timeFormatOnly.format(Date()), summary, detail))
            logAdapter.notifyItemInserted(logList.size - 1)
            rvLog?.scrollToPosition(logList.size - 1)
        }
    }

    fun showConnectingLoading() {
        if (!isAdded) return
        activity?.runOnUiThread {
            tvStatus?.text = "正在连接..."
            viewStatusDot?.setBackgroundResource(R.drawable.shape_dot_connecting)
            btnConnect?.text = "连接中..."
            btnConnect?.isEnabled = false 
        }
    }

    fun updateConnectionState(isConnected: Boolean) {
        activity?.runOnUiThread {
            btnConnect?.isEnabled = true 
            
            if (isConnected) {
                tvStatus?.text = "已连接"
                viewStatusDot?.setBackgroundResource(R.drawable.shape_dot_connected)
                
                btnConnect?.text = "断开连接"
                btnConnect?.backgroundTintList = ColorStateList.valueOf("#B00020".toColorInt())
                btnConnect?.setTextColor(Color.WHITE)
            } else {
                tvStatus?.text = "未连接"
                viewStatusDot?.setBackgroundResource(R.drawable.shape_dot_disconnected)
                
                btnConnect?.text = "连接服务器"
                // 恢复默认颜色
                try {
                    val bgColor = getThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
                    val textColor = getThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
                    btnConnect?.backgroundTintList = ColorStateList.valueOf(bgColor)
                    btnConnect?.setTextColor(textColor)
                } catch (_: Exception) {}
            }
            // 打卡按钮需要已连接且有用户
            val presenter = (activity as? MainActivity)?.presenter
            val hasUsers = presenter?.hasUsers() ?: false
            btnSimulateActive?.isEnabled = isConnected && hasUsers
        }
    }

    fun updateUserList(users: List<User>) {
        if (!isAdded) return
        activity?.runOnUiThread {
            userAdapter.clear()
            userAdapter.addAll(users)
            userAdapter.notifyDataSetChanged()
            
            // 同步更新控件启用状态
            val hasUsers = users.isNotEmpty()
            spinnerUser?.isEnabled = hasUsers
            btnDeleteUser?.isEnabled = hasUsers
            
            // 没有用户时禁用打卡
            val presenter = (activity as? MainActivity)?.presenter
            val isConnected = presenter?.isConnected() ?: false
            btnSimulateActive?.isEnabled = hasUsers && isConnected
        }
    }

    @SuppressLint("SetTextI18n")
    fun showSmartTimeSelection(amTime: Long, pmTime: Long, defaultIsAm: Boolean) {
        if (!isAdded) return
        activity?.runOnUiThread {
            cgSmartTime?.visibility = View.VISIBLE
            chipAm?.text = "上午 ${timeFormatShort.format(Date(amTime))}"
            chipAm?.tag = amTime
            chipPm?.text = "下午 ${timeFormatShort.format(Date(pmTime))}"
            chipPm?.tag = pmTime
            
            val idToSelect = if (defaultIsAm) R.id.chipAm else R.id.chipPm
            val timeToSelect = if (defaultIsAm) amTime else pmTime
            
            val presenter = (activity as MainActivity).presenter
            
            if (cgSmartTime?.checkedChipId == idToSelect) {
                presenter.setCustomTime(timeToSelect)
            } else {
                cgSmartTime?.check(idToSelect)
            }
        }
    }

    fun hideSmartTimeSelection() {
        if (!isAdded) return
        activity?.runOnUiThread {
            cgSmartTime?.visibility = View.GONE
            cgSmartTime?.clearCheck()
        }
    }

    fun updateTimeDisplay(str: String, tip: String?) {
        if (!isAdded) return
        activity?.runOnUiThread {
            tvSelectedTime?.text = if (tip != null) "推荐: $str" else str
            chipRuleTip?.visibility = if (tip != null) View.VISIBLE else View.GONE
            chipRuleTip?.text = tip
        }
    }

    private fun showDateTimePicker(presenter: MainContract.Presenter) {
        val c = Calendar.getInstance()
        android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m); c.set(Calendar.DAY_OF_MONTH, d)
            android.app.TimePickerDialog(requireContext(), { _, h, min ->
                c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min)
                val np = NumberPicker(requireContext()).apply { minValue=0; maxValue=59; value=c.get(Calendar.SECOND) }
                val fl = FrameLayout(requireContext()).apply { addView(np, FrameLayout.LayoutParams(-2, -2, 17)) }
                AlertDialog.Builder(requireContext()).setTitle("选择秒").setView(fl).setPositiveButton("确定"){_,_->
                    c.set(Calendar.SECOND, np.value)
                    presenter.setCustomTime(c.timeInMillis)
                }.setNegativeButton("取消", null).show()
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
}
