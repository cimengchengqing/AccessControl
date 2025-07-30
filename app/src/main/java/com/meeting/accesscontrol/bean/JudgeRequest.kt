package com.meeting.accesscontrol.bean

data class JudgeRequest(
    val userId: Long,    //用户ID 编号
    val meetingId: Long  //会议Id
)
