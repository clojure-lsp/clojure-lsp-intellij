package com.github.clojure_lsp.intellij.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler

/**
 * Editor action handler that gives paredit the first chance to handle the action
 * at each caret inside a write action and falls back to the platform handler it
 * replaces.
 */
abstract class PareditActionHandler(val original: EditorActionHandler) : EditorWriteActionHandler(true) {
    abstract fun handle(editor: Editor, caret: Caret?, dataContext: DataContext?): Boolean

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        if (!handle(editor, caret, dataContext)) {
            original.execute(editor, caret, dataContext)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        return original.isEnabled(editor, caret, dataContext)
    }
}

/**
 * Like [PareditActionHandler] but called once for all carets, for handlers that
 * only decide whether the platform handler may run, or run it themselves.
 */
abstract class PareditWriteGuardHandler(val original: EditorActionHandler) : EditorWriteActionHandler() {
    abstract fun handle(editor: Editor, caret: Caret?, dataContext: DataContext?): Boolean

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        if (!handle(editor, caret, dataContext)) {
            original.execute(editor, caret, dataContext)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        return original.isEnabled(editor, caret, dataContext)
    }
}

/**
 * Like [PareditWriteGuardHandler] but without a write action, for platform
 * handlers that manage their own, like cut and paste.
 */
abstract class PareditGuardHandler(val original: EditorActionHandler) : EditorActionHandler() {
    abstract fun handle(editor: Editor, caret: Caret?, dataContext: DataContext?): Boolean

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        if (!handle(editor, caret, dataContext)) {
            original.execute(editor, caret, dataContext)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        return original.isEnabled(editor, caret, dataContext)
    }
}
