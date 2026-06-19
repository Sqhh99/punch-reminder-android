package com.sqhh99.punchreminder.data.datastore

import kotlinx.serialization.Serializable

/**
 * timor.tech 节假日 API（`https://timor.tech/api/holiday/year/{year}`）的响应模型。
 *
 * 返回体形如：
 * ```
 * { "code": 0,
 *   "holiday": {
 *     "10-01": { "holiday": true,  "name": "国庆节", "date": "2026-10-01" },
 *     "10-11": { "holiday": false, "name": "国庆节后调休", "date": "2026-10-11" }
 *   } }
 * ```
 * - `holiday` map 只列出特殊日：key 为 "MM-dd"。
 * - 条目 `holiday=true` → 法定放假日；`holiday=false` → 调休上班日；未列出的日期为普通日。
 */
@Serializable
data class TimorYearResponse(
    val code: Int = -1,
    val holiday: Map<String, TimorDay> = emptyMap(),
)

@Serializable
data class TimorDay(
    val holiday: Boolean = false,
    val name: String = "",
    /** 完整日期 "yyyy-MM-dd"，优先用作缓存 key 来源。 */
    val date: String = "",
)
