package com.mytvb.model

import java.io.Serializable

data class SettingModel(
    var title: String = "",
    var info: String = "",
    /** 持久化存储值（稳定中文字面量/数字，不随界面语言变化）；info 仅作本地化显示。 */
    var value: String = ""
) : Serializable
