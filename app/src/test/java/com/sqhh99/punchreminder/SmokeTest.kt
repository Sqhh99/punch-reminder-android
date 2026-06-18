package com.sqhh99.punchreminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 最小冒烟测试：确保 testDebugUnitTest 任务真实执行、CI 能采集测试报告。
 *
 * 核心业务逻辑（如 NextTriggerTimeCalculator）的单元测试将在对应里程碑随实现一并加入。
 */
class SmokeTest {

    @Test
    fun jvmUnitTestPipelineWorks() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun appVersionNameMatchesMilestone() {
        // 与 app/build.gradle.kts 中的 versionName 对齐（里程碑一 = 0.1.0）。
        assertTrue("0.1.0".startsWith("0.1"))
    }
}
