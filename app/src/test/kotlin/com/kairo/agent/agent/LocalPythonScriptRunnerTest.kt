package com.kairo.agent.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPythonScriptRunnerTest {

    @Test
    fun `evaluates arithmetic, variables, and print`() {
        val script = """
            price = 49.50
            tax = 1.21
            total = round(price * tax, 2)
            print("Total:", total)
        """.trimIndent()
        val res = LocalPythonScriptRunner.runSandboxed(script)
        assertTrue(res.success)
        assertEquals("Total: 59.9", res.output)
    }

    @Test
    fun `evaluates math functions and list sum`() {
        val script = """
            import math
            vals = [10, 20, 30]
            round(math.sqrt(sum(vals) + 4), 2)
        """.trimIndent()
        val res = LocalPythonScriptRunner.runSandboxed(script)
        assertTrue(res.success)
        assertEquals("8", res.output)
    }

    @Test
    fun `confirms current date and time`() {
        val res = LocalPythonScriptRunner.runSandboxed("datetime.now()")
        assertTrue(res.success)
        assertTrue(res.output.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} .*""")))
    }
}
