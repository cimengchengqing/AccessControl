package com.meeting.accesscontrol.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.meeting.accesscontrol.R
import com.meeting.accesscontrol.bean.Meeting
import com.meeting.accesscontrol.tools.TimeFormatter

class MeetingListAdapter(
    private val context: Context,
    private var recordFiles: List<Meeting>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = recordFiles.size

    override fun getItem(position: Int): Meeting = recordFiles[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: inflater.inflate(R.layout.item_order_meeting, parent, false)

        val viewHolder = if (convertView == null) {
            ViewHolder(
                titleTv = view.findViewById(R.id.meeting_title),
                dateTv = view.findViewById(R.id.meeting_date),
                initiatorTv = view.findViewById(R.id.meeting_initiator),
            ).also { view.tag = it }
        } else {
            view.tag as ViewHolder
        }

        val recordFile = getItem(position)
        bindData(viewHolder, recordFile)

        return view
    }

    private fun bindData(viewHolder: ViewHolder, bean: Meeting) {
        viewHolder.titleTv.text = "会议主题：${bean.title}"
        viewHolder.dateTv.text =
            "会议时间：${TimeFormatter.formatSameDayTimeRange(bean.startTime, bean.endTime)}"
        viewHolder.initiatorTv.text = "发起人：${bean.promoter}"
    }

    fun updateData(newRecordFiles: List<Meeting>) {
        this.recordFiles = newRecordFiles
        notifyDataSetChanged()
    }

    private data class ViewHolder(
        val titleTv: TextView,
        val dateTv: TextView,
        val initiatorTv: TextView
    )


}
