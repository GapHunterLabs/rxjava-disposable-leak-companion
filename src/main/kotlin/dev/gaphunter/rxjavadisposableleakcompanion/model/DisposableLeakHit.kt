package dev.gaphunter.rxjavadisposableleakcompanion.model

import com.intellij.psi.PsiElement

/** One `.subscribe(...)` call whose returned Disposable is discarded as a bare expression statement. */
data class DisposableLeakHit(val callElement: PsiElement)
