package com.meeting.accesscontrol.bean

data class EntranceGuardInfo(
    val guard_id: Long,     //门禁点Id
    val guard_name: String, //门禁点名称
    val device_no: String,  //门禁设备编号
    val device_name: String,    //门禁设备名称
    val guard_status: String,   //门禁点状态
    val online_status: String  //设备在线状态
)

