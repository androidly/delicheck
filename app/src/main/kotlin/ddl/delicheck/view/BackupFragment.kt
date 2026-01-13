package ddl.delicheck.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import ddl.delicheck.R

class BackupFragment : Fragment(R.layout.fragment_backup) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val presenter = (requireActivity() as MainActivity).presenter

        view.findViewById<View>(R.id.btnExport).setOnClickListener {
            presenter.requestExport()
        }

        view.findViewById<View>(R.id.btnImport).setOnClickListener {
            showImportInput()
        }
    }

    fun showExportResult(json: String) {
        if (!isAdded) return
        val et = EditText(context).apply { setText(json); setSelectAllOnFocus(true) }
        AlertDialog.Builder(requireContext()).setTitle("配置代码").setView(et)
            .setPositiveButton("复制") { _, _ ->
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Config", json))
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }.show()
    }

    fun showImportInput() {
        if (!isAdded) return
        val et = EditText(context).apply { hint = "粘贴代码..." }
        AlertDialog.Builder(requireContext()).setTitle("导入").setView(et)
            .setPositiveButton("确定") { _, _ ->
                (requireActivity() as MainActivity).presenter.performImport(et.text.toString().trim())
            }.show()
    }
}
