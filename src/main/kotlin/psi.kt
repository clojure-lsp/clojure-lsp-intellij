/*
 * Copyright 2016-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.clojure_lsp.intellij.language.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.ILeafElementType
import com.github.clojure_lsp.intellij.ClojureLanguage
import com.github.clojure_lsp.intellij.ClojureFileType

interface ClojureElementType
class ClojureTokenType(name: String) : IElementType(name, ClojureLanguage), ILeafElementType {
  override fun createLeafNode(leafText: CharSequence) = CToken(this, leafText)
}
class ClojureNodeType(name: String) : IElementType(name, ClojureLanguage), ClojureElementType
class CToken(tokenType: ClojureTokenType, text: CharSequence) : LeafPsiElement(tokenType, text), PsiNameIdentifierOwner {
    override fun getNameIdentifier() = this
    override fun getName() = this.getText()
    override fun setName(name: String) = this
}

open class CFileImpl(viewProvider: FileViewProvider, language: Language) :
    PsiFileBase(viewProvider, language), PsiFile {

  override fun getFileType() = ClojureFileType
  override fun toString() = "${javaClass.simpleName}:$name"
}

interface CElement : NavigatablePsiElement {}
