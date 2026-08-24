package dev.gaphunter.rxjavadisposableleakcompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import dev.gaphunter.rxjavadisposableleakcompanion.detect.JavaSubscribeStatementFinder
import dev.gaphunter.rxjavadisposableleakcompanion.detect.KotlinSubscribeStatementFinder
import dev.gaphunter.rxjavadisposableleakcompanion.model.DisposableLeakHit
import dev.gaphunter.rxjavadisposableleakcompanion.review.ReviewPrompt

class DisposableLeakLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "RxJava subscribe() Disposable discarded"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val hits = when (file.language.id) {
            "JAVA" -> JavaSubscribeStatementFinder.findAll(file)
            "kotlin" -> KotlinSubscribeStatementFinder.findAll(file)
            else -> emptyList()
        }
        if (hits.isEmpty()) return

        val hitsByElement = hits.associateBy { it.callElement }
        for (element in elements) {
            val hit = hitsByElement[element] ?: continue
            result.add(buildMarker(hit))

            val path = file.virtualFile?.path ?: continue
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
        }
    }

    private fun buildMarker(hit: DisposableLeakHit): LineMarkerInfo<PsiElement> {
        val tooltip = "The Disposable returned by subscribe() here is discarded -- there is no way to call " +
            "dispose() on this subscription later. RxJava's own guidance: always dispose of subscriptions when " +
            "no longer needed, or this is a well-documented source of memory/resource leaks"
        return LineMarkerInfo(
            hit.callElement,
            hit.callElement.textRange,
            DisposableLeakIcons.RISK,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }
}
