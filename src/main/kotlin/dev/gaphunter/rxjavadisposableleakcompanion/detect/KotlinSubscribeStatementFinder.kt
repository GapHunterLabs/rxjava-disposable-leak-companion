package dev.gaphunter.rxjavadisposableleakcompanion.detect

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.rxjavadisposableleakcompanion.model.DisposableLeakHit
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/** Kotlin counterpart of [JavaSubscribeStatementFinder]. */
object KotlinSubscribeStatementFinder {

    fun findAll(file: PsiFile): List<DisposableLeakHit> {
        if (file !is KtFile) return emptyList()
        val hits = mutableListOf<DisposableLeakHit>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitBlockExpression(expression: KtBlockExpression) {
                super.visitBlockExpression(expression)
                for (statement in expression.statements) {
                    hitFor(statement)?.let { hits += it }
                }
            }
        })
        return hits
    }

    private fun hitFor(statement: PsiElement): DisposableLeakHit? {
        val dotExpr = statement as? KtDotQualifiedExpression ?: return null
        val call = dotExpr.selectorExpression as? KtCallExpression ?: return null
        if (call.calleeExpression?.text != "subscribe") return null

        val valueArgs = call.valueArguments.filter { it !is KtLambdaArgument }
        val lambdaArgs = call.lambdaArguments
        val totalArgCount = valueArgs.size + lambdaArgs.size
        if (totalArgCount > 4) return null

        if (totalArgCount == 1) {
            // A single non-lambda argument that looks like an object
            // instantiation (an anonymous object literal, a class
            // reference, or a plain identifier naming a
            // DisposableObserver-typed variable) is the
            // void-returning subscribe(Observer) overload -- excluded.
            val onlyArg = valueArgs.firstOrNull()?.getArgumentExpression()
            if (onlyArg is KtObjectLiteralExpression || onlyArg is KtClassLiteralExpression) return null
            // A callable reference (::onNext) is lambda-like; anything
            // else standing alone as the sole argument (a plain
            // identifier naming an Observer-typed variable, etc.) is
            // treated as the void-returning subscribe(Observer)
            // overload -- excluded.
            if (lambdaArgs.isEmpty() && onlyArg != null && onlyArg !is KtCallableReferenceExpression) return null
        }

        return DisposableLeakHit(leafOf(dotExpr))
    }

    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
