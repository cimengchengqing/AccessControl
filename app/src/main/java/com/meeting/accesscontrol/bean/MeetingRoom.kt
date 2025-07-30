package com.meeting.accesscontrol.bean

data class MeetingRoom(
    val deviceId: String,   //会议室对应的门禁编码
    val roomId: String,     //会议室ID
    val roomName: String,     //会议室名称
    val meetingInfoList: List<Meeting>   //会议列表信息
)

data class Meeting(
    val title: String,      //会议主题
    val promoter: String,     //发起人名字
    val meetingId: String,     //会议ID
    val startTime: Long,
    val endTime: Long,
    val userInfoList: List<UserInfo>
)

data class UserInfo(
    val name: String
)
