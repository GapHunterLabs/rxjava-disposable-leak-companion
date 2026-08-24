package dev.gaphunter.rxjavadisposableleakcompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinSubscribeStatementFinderTest : BasePlatformTestCase() {

    fun `test subscribe with lambda callbacks discarded is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    orders.subscribe({ order -> process(order) }, { error -> log(error) })
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinSubscribeStatementFinder.findAll(file).size)
    }

    fun `test subscribe with trailing lambda discarded is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    orders.subscribe { order -> process(order) }
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinSubscribeStatementFinder.findAll(file).size)
    }

    fun `test subscribe assigned to a val is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    val d = orders.subscribe { order -> process(order) }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinSubscribeStatementFinder.findAll(file).isEmpty())
    }

    fun `test subscribe with a single Observer instance argument is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    orders.subscribe(observer)
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinSubscribeStatementFinder.findAll(file).isEmpty())
    }
}
