package org.eclipse.imp.services;

import org.eclipse.imp.language.ILanguageService;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2004  All Rights Reserved
 */


/**
 * This interface is to be implemented by contributors to the org.eclipse.imp.runtime.contentProposer extension point.
 * The Universal IDE Editor will locate a suitable parser for the language being edited.
 * The result of the parser, an Ast describing the syntactical elements in the input, is cached
 * and used when the mouse is hovered over elements in the editor.
 * 
 * @author Claffra
 */
public interface IContentProposer extends ILanguageService {

    public ICompletionProposal[] getContentProposals(IParseController controller, int offset, ITextViewer viewer);

}