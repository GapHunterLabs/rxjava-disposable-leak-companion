package dev.gaphunter.rxjavadisposableleakcompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaSubscribeStatementFinderTest : BasePlatformTestCase() {

    fun `test subscribe with lambda callbacks discarded is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    orders.subscribe(order -> process(order), error -> log(error));
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaSubscribeStatementFinder.findAll(file).size)
    }

    fun `test subscribe with no arguments discarded is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    orders.subscribe();
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaSubscribeStatementFinder.findAll(file).size)
    }

    fun `test subscribe assigned to a Disposable variable is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    Disposable d = orders.subscribe(order -> process(order));
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaSubscribeStatementFinder.findAll(file).isEmpty())
    }

    fun `test subscribe with a single Observer instance argument is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    orders.subscribe(new DisposableObserver<Order>() {
                        public void onNext(Order order) { process(order); }
                        public void onError(Throwable e) { }
                        public void onComplete() { }
                    });
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaSubscribeStatementFinder.findAll(file).isEmpty())
    }

    fun `test unrelated subscribe method on some other type is still flagged as a possible false positive by design`() {
        // Documented v0.1 limitation: matches by simple method name, not
        // real type resolution.
        val file = myFixture.configureByText(
            "NewsletterService.java",
            """
            class NewsletterService {
                void signUp() {
                    mailingList.subscribe(email -> send(email));
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaSubscribeStatementFinder.findAll(file).size)
    }
}
