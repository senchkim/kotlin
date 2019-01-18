/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.lang.*
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.options.colors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.tree.*
import java.io.Reader
import javax.swing.Icon
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.konan.library.KDEFINITIONS_FILE_EXTENSION
import org.jetbrains.kotlin.ide.konan.psi.NativeDefinitionsFile
import org.jetbrains.kotlin.ide.konan.psi.impl.NativeDefinitionsTypes


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

class NativeDefinitionsColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon? = KotlinIcons.NATIVE

    override fun getHighlighter(): SyntaxHighlighter = NativeDefinitionsSyntaxHighlighter()

    override fun getDemoText(): String {
        return """headers = curl/curl.h
# Unspecified options.
linkerOpts = -DBAR=bar
! Specified options.
compilerOpts.linux = -I/usr/include -I/usr/include/x86_64-linux-gnu

---

struct Hash { // Line comment.
    int data[2];
};

/**********************
 * Multiline comment. *
 **********************/
inline static int strangeSum(const int* buffer, int bufferSize) {
    int result = 20;
    const char * stringLiteral = "This is a string";
    for (int i = 0; i < bufferSize; ++i) {
        result += stringLiteral[i % 10] & 1 == 0 ? (i << 1) : buffer[i / 2];
    }
    return result;
}
"""
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        infix fun String.to(keys: Array<TextAttributesKey>) = AttributesDescriptor(this, keys[0])

        return arrayOf(
            "Comment" to NativeDefinitionsSyntaxHighlighter.DELIM_KEYS,
            "Identifier" to NativeDefinitionsSyntaxHighlighter.INJECTION_KEYS
        )
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = NATIVE_DEFINITIONS_NAME
}