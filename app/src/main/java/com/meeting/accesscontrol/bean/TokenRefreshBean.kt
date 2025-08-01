package com.meeting.accesscontrol.bean

data class TokenRefreshBean(
    val access_token: String,   //token值
    val expires_in: Int        //token有效时长:单位/s，一般为7200秒（即两小时）
)