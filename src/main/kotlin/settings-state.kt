package com.github.clojure_lsp.intellij.extension

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(storages = [Storage("ClojureLSPSettings.xml")], name = "ClojureLSPSettingsState")
class SettingsState : PersistentStateComponent<SettingsState?> {
    var traceLevel: String? = null
    var serverLogPath: String? = null
    var serverPath: String? = null

    override fun getState(): SettingsState? {
        return this
    }

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun get(): SettingsState = ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
