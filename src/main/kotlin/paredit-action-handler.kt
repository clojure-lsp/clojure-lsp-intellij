package com.github.clojure_lsp.intellij.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler

/**
 * Editor action handler that gives paredit the first chance to handle the action
 * and falls back to the platform handler it replaces.
 */
abstract class PareditActionHandler(private val original: EditorActionHandler) : EditorWriteActionHandler() {
    abstract fun handle(editor: Editor): Boolean

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        if (!handle(editor)) {
            original.execute(editor, caret, dataContext)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        return original.isEnabled(editor, caret, dataContext)
    }
}
