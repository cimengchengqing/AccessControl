package com.meeting.accesscontrol.bean

data class RoomInfo(
    val code: Int,      //房间类型：1-会议室，2-办公室
    val desc: String,   //房间名称
    val guardId: String //门禁ID
)
