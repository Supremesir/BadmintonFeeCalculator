package com.supremesir.badmintonfeecalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun String.safeToDouble() = this.toDoubleOrNull() ?: 0.0

fun String.safeToInt() = this.toIntOrNull() ?: 0

// 点击事件处理函数
fun copyOnClick(context: Context, text: String) {
    // 获取剪贴板管理器
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // 创建剪贴板数据
    val clipData = ClipData.newPlainText("Copied Text", text)

    // 将剪贴板数据设置到剪贴板管理器中
    clipboardManager.setPrimaryClip(clipData)

    // 展示弹窗
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}