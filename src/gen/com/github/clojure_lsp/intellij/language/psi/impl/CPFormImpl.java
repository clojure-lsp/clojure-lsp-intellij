// This is a generated file. Not intended for manual editing.
package com.github.clojure_lsp.intellij.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.clojure_lsp.intellij.language.psi.ClojureTypes.*;
import com.github.clojure_lsp.intellij.language.psi.*;

public class CPFormImpl extends CFormImpl implements CPForm {

  public CPFormImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull ClojureVisitor visitor) {
    visitor.visitPForm(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ClojureVisitor) accept((ClojureVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<CForm> getForms() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CForm.class);
  }

}
