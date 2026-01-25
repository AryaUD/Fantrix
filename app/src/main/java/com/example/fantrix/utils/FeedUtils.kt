package com.example.fantrix.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatTimeAgo(date: Date): String {
    val diff = Date().time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 7 -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "Just now"
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}