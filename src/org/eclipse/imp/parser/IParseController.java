package org.eclipse.uide.parser;

import java.util.List;
import lpg.javaruntime.IToken;
import lpg.javaruntime.IMessageHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.uide.core.ILanguageService;

public interface IParseController extends ILanguageService
{
    void initialize(String projRelFilePath, IProject project, IMessageHandler handler);

    IParser getParser();

    ILexer getLexer();

    Object getCurrentAst();

    boolean isKeyword(int kind);

    char [][] getKeywords();

    public int getTokenIndexAtCharacter(int offset);

    public IToken getTokenAtCharacter(int offset);

    IASTNodeLocator getNodeLocator();

    boolean hasErrors();

    /**
     * @return a List of ParseError
     */
    List getErrors();

    Object parse(String input, boolean scanOnly, IProgressMonitor monitor);
}