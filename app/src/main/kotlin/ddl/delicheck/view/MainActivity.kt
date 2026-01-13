package ddl.delicheck.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ddl.delicheck.R
import ddl.delicheck.databinding.ActivityMainBinding
import ddl.delicheck.model.AppConfig
import ddl.delicheck.model.PreferenceManager
import ddl.delicheck.model.User
import ddl.delicheck.presenter.MainPresenter
import androidx.core.view.get

class MainActivity : AppCompatActivity(), MainContract.View {

    lateinit var binding: ActivityMainBinding
    lateinit var presenter: MainContract.Presenter

    val homeFragment = HomeFragment()
    val backupFragment = BackupFragment()
    val settingsFragment = SettingsFragment()

    // Toast定时器
    private val toastHandler = Handler(Looper.getMainLooper())
    private val hideToastRunnable = Runnable { hideCustomToast() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        presenter = MainPresenter(this, PreferenceManager(this))
        setupNavigation()
        presenter.start()
    }

    private fun setupNavigation() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> homeFragment
                    1 -> backupFragment
                    2 -> settingsFragment
                    else -> homeFragment
                }
            }
        }
        binding.viewPager.offscreenPageLimit = 2

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavigation.menu[position].isChecked = true
                binding.topAppBar.title = when(position) {
                    0 -> "打卡"
                    1 -> "备份导入导出"
                    2 -> "高级设置"
                    else -> ""
                }
            }
        })

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.currentItem = 0
                R.id.nav_backup -> binding.viewPager.currentItem = 1
                R.id.nav_settings -> binding.viewPager.currentItem = 2
            }
            true
        }
    }

    // 自定义Toast动画
    override fun showToast(msg: String) {
        runOnUiThread {
            // 取消已有的隐藏任务
            toastHandler.removeCallbacks(hideToastRunnable)

            binding.tvToastMsg.text = msg

            // 如果隐藏，先重置
            if (binding.cvCustomToast.visibility != View.VISIBLE) {
                binding.cvCustomToast.alpha = 0f
                binding.cvCustomToast.translationY = 100f
                binding.cvCustomToast.visibility = View.VISIBLE
            }

            // 入场动画
            binding.cvCustomToast.animate()
                .alpha(1f)
                .translationY(0f) // 回到原位
                .setDuration(300)
                .start()

            // 2秒后隐藏
            toastHandler.postDelayed(hideToastRunnable, 2000)
        }
    }

    private fun hideCustomToast() {
        binding.cvCustomToast.animate()
            .alpha(0f)
            .translationY(100f) // 下沉 100px
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { 
                binding.cvCustomToast.visibility = View.GONE 
            }
            .start()
    }

    // View接口实现

    override fun addLog(summary: String, detail: String?) {
        homeFragment.addLog(summary, detail)
    }

    override fun showConnectingLoading() {
        homeFragment.showConnectingLoading()
    }

    override fun updateConnectionState(isConnected: Boolean) {
        homeFragment.updateConnectionState(isConnected)
    }

    override fun updateUserList(users: List<User>) {
        homeFragment.updateUserList(users)
    }

    override fun updateTimeDisplay(timeStr: String, ruleTip: String?) {
        homeFragment.updateTimeDisplay(timeStr, ruleTip)
    }

    override fun showSmartTimeSelection(amTime: Long, pmTime: Long, defaultSelectionIsAm: Boolean) {
        homeFragment.showSmartTimeSelection(amTime, pmTime, defaultSelectionIsAm)
    }

    override fun hideSmartTimeSelection() {
        homeFragment.hideSmartTimeSelection()
    }

    override fun showAdvancedSettings(config: AppConfig) {
        settingsFragment.displayConfig(config)
    }

    override fun showExportDialog(json: String) {
        backupFragment.showExportResult(json)
    }

    override fun showImportDialog() {
        backupFragment.showImportInput()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
