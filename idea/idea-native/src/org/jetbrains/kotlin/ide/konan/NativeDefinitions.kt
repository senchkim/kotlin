/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.lang.*
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.options.colors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.tree.*
import java.io.Reader
import javax.swing.Icon
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.konan.library.KDEFINITIONS_FILE_EXTENSION
import org.jetbrains.kotlin.ide.konan.psi.NativeDefinitionsFile
import org.jetbrains.kotlin.ide.konan.psi.impl.NativeDefinitionsFirstHalfImpl
import org.jetbrains.kotlin.ide.konan.psi.impl.NativeDefinitionsTypes
import java.lang.StringBuilder


const val NATIVE_DEFINITIONS_NAME = "KND"
const val NATIVE_DEFINITIONS_DESCRIPTION = "Definitions file for Kotlin/Native C interop"

class NativeDefinitionsFileType : LanguageFileType(NativeDefinitionsLanguage.INSTANCE) {

    override fun getName(): String = NATIVE_DEFINITIONS_NAME

    override fun getDescription(): String = NATIVE_DEFINITIONS_DESCRIPTION

    override fun getDefaultExtension(): String = KDEFINITIONS_FILE_EXTENSION

    override fun getIcon(): Icon = KotlinIcons.NATIVE

    companion object {
        val INSTANCE = NativeDefinitionsFileType()
    }
}

class NativeDefinitionsLanguage private constructor() : Language(NATIVE_DEFINITIONS_NAME) {
    companion object {
        val INSTANCE = NativeDefinitionsLanguage()
    }
}

class NativeDefinitionsLexerAdapter : FlexAdapter(NativeDefinitionsLexer(null as Reader?))

class NativeDefinitionsParserDefinition : ParserDefinition {
    private val FILE = IFileElementType(NativeDefinitionsLanguage.INSTANCE)

    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun getFileNodeType(): IFileElementType = FILE

    override fun createLexer(project: Project): Lexer = NativeDefinitionsLexerAdapter()
    override fun createParser(project: Project): PsiParser = NativeDefinitionsParser()
    override fun createFile(viewProvider: FileViewProvider): PsiFile = NativeDefinitionsFile(viewProvider)
    override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
        ParserDefinition.SpaceRequirements.MAY

    override fun createElement(node: ASTNode): PsiElement = NativeDefinitionsTypes.Factory.createElement(node)
}

class NativeDefinitionsSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            NativeDefinitionsTypes.FIRST_HALF -> INJECTION_KEYS
            NativeDefinitionsTypes.SECOND_HALF -> INJECTION_KEYS
            NativeDefinitionsTypes.DELIM -> DELIM_KEYS
            else -> EMPTY_KEYS
        }

    override fun getHighlightingLexer(): Lexer = NativeDefinitionsLexerAdapter()

    companion object {
        private fun createKeys(externalName: String, key: TextAttributesKey): Array<TextAttributesKey> {
            return arrayOf(TextAttributesKey.createTextAttributesKey(externalName, key))
        }

        val DELIM_KEYS = createKeys("Comment", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val INJECTION_KEYS = createKeys("String literal", DefaultLanguageHighlighterColors.STRING)
        val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}

class NativeDefinitionsSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        NativeDefinitionsSyntaxHighlighter()
}

class PropertiesEscaper(host: NativeDefinitionsFirstHalfImpl): LiteralTextEscaper<NativeDefinitionsFirstHalfImpl>(host) {
    override fun isOneLine(): Boolean = false

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int = offsetInDecoded

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        outChars.append(myHost.text)
        return true
    }

}

class PropertiesLanguageInjector: MultiHostInjector {
    val propertiesLanguage = Language.findLanguageByID("Properties");

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val ktHost = context as? NativeDefinitionsFirstHalfImpl ?: return
        if (!context.isValidHost || propertiesLanguage == null) return

        registrar.startInjecting(propertiesLanguage).addPlace(null,null, context, context.getTextRange()).doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(NativeDefinitionsFirstHalfImpl::class.java)
    }
}