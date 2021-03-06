/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.impulse.services;

import org.eclipse.swt.graphics.Point;

import io.usethesource.impulse.parser.IParseController;

public interface IASTFindReplaceTarget {
    String getSelectionText();

    boolean isEditable();

    /**
     * x coordinate is offset of start of selection, y coordinate is length of selection
     */
    Point getSelection();

    boolean canPerformFind();

    IParseController getParseController();
}
