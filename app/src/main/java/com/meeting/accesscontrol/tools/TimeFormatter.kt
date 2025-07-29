package com.meeting.accesscontrol.tools

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 时间格式化工具类
 */
object TimeFormatter {


    /**
     * 获取时间戳的时分并输出：HH:MM
     */
    fun getHoursForTimestamp(timestamp: Long): String {
        val time = timestamp / 1000
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault())
        return String.format("%02d:%02d", dateTime.hour, dateTime.minute)
    }

    /**
     * 起止时间格式化 - 优化版本
     * @param start 开始时间戳（毫秒）
     * @param end 结束时间戳（毫秒）
     * @return 格式化后的时间字符串，格式：2025-07-22 11:00-12:00
     */
    fun formatSameDayTimeRange(start: Long, end: Long): String {
        return try {
            // 使用Instant处理时间戳，避免手动计算
            val startInstant = Instant.ofEpochMilli(start)
            val endInstant = Instant.ofEpochMilli(end)

            // 转换为本地时间（使用系统默认时区）
            val zoneId = ZoneId.systemDefault()
            val startDateTime = LocalDateTime.ofInstant(startInstant, zoneId)
            val endDateTime = LocalDateTime.ofInstant(endInstant, zoneId)

            // 定义格式化器
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            // 格式化日期和时间
            val dateStr = startDateTime.format(dateFormatter)
            val startTimeStr = startDateTime.format(timeFormatter)
            val endTimeStr = endDateTime.format(timeFormatter)

            "$dateStr $startTimeStr-$endTimeStr"
        } catch (e: Exception) {
            // 错误处理，返回默认格式
            "时间格式错误"
        }
    }

    /**
     * 起止时间格式化 - 支持自定义时区
     * @param start 开始时间戳（毫秒）
     * @param end 结束时间戳（毫秒）
     * @param zoneId 时区ID，默认为系统时区
     * @return 格式化后的时间字符串
     */
    fun formatSameDayTimeRange(start: Long, end: Long, zoneId: ZoneId): String {
        return try {
            val startInstant = Instant.ofEpochMilli(start)
            val endInstant = Instant.ofEpochMilli(end)

            val startDateTime = LocalDateTime.ofInstant(startInstant, zoneId)
            val endDateTime = LocalDateTime.ofInstant(endInstant, zoneId)

            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val dateStr = startDateTime.format(dateFormatter)
            val startTimeStr = startDateTime.format(timeFormatter)
            val endTimeStr = endDateTime.format(timeFormatter)

            "$dateStr $startTimeStr-$endTimeStr"
        } catch (e: Exception) {
            "时间格式错误"
        }
    }

    /**
     * 起止时间格式化 - 支持自定义格式
     * @param start 开始时间戳（毫秒）
     * @param end 结束时间戳（毫秒）
     * @param datePattern 日期格式，默认为 "yyyy-MM-dd"
     * @param timePattern 时间格式，默认为 "HH:mm"
     * @return 格式化后的时间字符串
     */
    fun formatSameDayTimeRange(
        start: Long,
        end: Long,
        datePattern: String = "yyyy-MM-dd",
        timePattern: String = "HH:mm"
    ): String {
        return try {
            val startInstant = Instant.ofEpochMilli(start)
            val endInstant = Instant.ofEpochMilli(end)

            val zoneId = ZoneId.systemDefault()
            val startDateTime = LocalDateTime.ofInstant(startInstant, zoneId)
            val endDateTime = LocalDateTime.ofInstant(endInstant, zoneId)

            val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
            val timeFormatter = DateTimeFormatter.ofPattern(timePattern)

            val dateStr = startDateTime.format(dateFormatter)
            val startTimeStr = startDateTime.format(timeFormatter)
            val endTimeStr = endDateTime.format(timeFormatter)

            "$dateStr $startTimeStr-$endTimeStr"
        } catch (e: Exception) {
            "时间格式错误"
        }
    }

    /**
     * 验证时间戳是否有效
     * @param timestamp 时间戳（毫秒）
     * @return 是否有效
     */
    private fun isValidTimestamp(timestamp: Long): Boolean {
        return timestamp > 0 && timestamp < Long.MAX_VALUE
    }
}