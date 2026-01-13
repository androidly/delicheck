package ddl.delicheck.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ddl.delicheck.R
import ddl.delicheck.model.LogItem

class LogAdapter(private val logs: List<LogItem>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvSummary: TextView = view.findViewById(R.id.tvSummary)
        val tvDetail: TextView = view.findViewById(R.id.tvDetail)
        val tvExpandIcon: TextView = view.findViewById(R.id.tvExpandIcon)
        val layoutSummary: View = view.findViewById(R.id.layoutSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = logs[position]
        holder.tvTime.text = item.time
        holder.tvSummary.text = item.summary
        if (item.detail.isNullOrEmpty()) {
            holder.tvExpandIcon.visibility = View.INVISIBLE
            holder.tvDetail.visibility = View.GONE
            holder.layoutSummary.setOnClickListener(null)
        } else {
            holder.tvExpandIcon.visibility = View.VISIBLE
            holder.tvDetail.text = item.detail
            holder.tvDetail.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            holder.tvExpandIcon.text = if (item.isExpanded) "▲" else "▼"
            holder.layoutSummary.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = logs.size
}
