package dev.gaphunter.rxjavadisposableleakcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiNewExpression
import dev.gaphunter.rxjavadisposableleakcompanion.model.DisposableLeakHit

/**
 * Finds `xxx.subscribe(...)` (RxJava 2.x/3.x `Observable`/`Flowable`/
 * `Single`/`Maybe`/`Completable`) written as a **bare expression
 * statement** -- the returned `Disposable` is discarded immediately,
 * with no way to ever call `.dispose()` on it later. RxJava's own
 * documentation is explicit that failing to dispose of a subscription
 * is a well-documented source of memory/resource leaks -- "always call
 * dispose() on your disposables when [the subscription] is no longer
 * needed".
 *
 * **v0.1 scope, stated honestly:** only flags the lambda/callback-based
 * `subscribe(...)` overloads (0 to 4 lambda/method-reference arguments),
 * which are the overloads that actually return `Disposable` -- the
 * single-argument `subscribe(Observer)`/`subscribe(DisposableObserver)`
 * overload returns `void`/`Unit` (disposal is handled differently, via
 * `Observer.onSubscribe`) and is deliberately never flagged: a single
 * argument that looks like an object instantiation (`new
 * DisposableObserver() {...}` or a named variable of a capitalized
 * type) is excluded. Matches by simple method name (`subscribe`), not
 * real type resolution -- an unrelated `subscribe()` method on some
 * other type is a possible (rare) false positive. Doesn't attempt to
 * trace whether the statement is immediately followed by adding the
 * (nonexistent, since it's discarded) reference to a
 * `CompositeDisposable` -- that's a contradiction by construction,
 * since the return value was never captured in the first place.
 */
object JavaSubscribeStatementFinder {

    fun findAll(file: PsiFile): List<DisposableLeakHit> {
        val hits = mutableListOf<DisposableLeakHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitExpressionStatement(statement: PsiExpressionStatement) {
                super.visitExpressionStatement(statement)
                hitFor(statement)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(statement: PsiExpressionStatement): DisposableLeakHit? {
        val call = statement.expression as? PsiMethodCallExpression ?: return null
        if (call.methodExpression.referenceName != "subscribe") return null
        // Must have a qualifier -- a bare `subscribe(...)` with no
        // receiver isn't the Rx pattern this plugin targets.
        call.methodExpression.qualifierExpression ?: return null

        val args = call.argumentList.expressions
        if (args.size > 4) return null // not a subscribe(...) callback overload this plugin recognizes
        val allLambdaLike = args.all { arg ->
            arg is PsiLambdaExpression || arg is PsiMethodReferenceExpression
        }
        // 0 arguments -- subscribe() -- is also a Disposable-returning
        // overload. Any non-empty argument list must be all
        // lambdas/method-references to be recognized; a single
        // `new XxxObserver() {...}` or a named Observer variable is
        // excluded (the void-returning overload).
        if (args.isNotEmpty() && !allLambdaLike) return null
        if (args.size == 1 && args[0] is PsiNewExpression) return null

        return DisposableLeakHit(leafOf(call))
    }

    /** Descends to a real leaf PSI element -- LineMarkerInfo must never anchor on a composite node (SDK_GOTCHAS.md SS20). */
    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
