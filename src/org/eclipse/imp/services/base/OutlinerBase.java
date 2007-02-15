	package org.eclipse.uide.defaults;

import java.util.ArrayList;
import java.util.Stack;

import lpg.lpgjavaruntime.IToken;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.uide.core.ErrorHandler;
import org.eclipse.uide.editor.IOutlineImage;
import org.eclipse.uide.editor.IOutliner;
import org.eclipse.uide.editor.UniversalEditor;
import org.eclipse.uide.parser.IASTNodeLocator;
import org.eclipse.uide.parser.IParseController;

/**
 * OutlinerBase is an abstract base type for a source-text outlining service.
 * It is intended to support extensions for language-specific outliners.
 * It is adapted from DefaultOutliner by Chris Laffra, but whereas that was
 * intended to provide an operational implementation of a generic outliner,
 * OutlinerBase explicitly requires completion by language-specific outliner
 * implementations.  This class is actually abstract only with respect to 
 * a method that sends a visitor to an AST, as both the visitor and AST types
 * are language-specific.
 * 
 * @see org.eclipse.uide.defaults.DefaultOutliner
 * 
 * @author 	suttons@us.ibm.com
 */
public abstract class OutlinerBase implements IOutliner
{
	
	protected UniversalEditor editor = null;
	
	public void setEditor(UniversalEditor editor) {
		this.editor = editor;
	}
	
	protected Tree tree;
	
	protected class TreeSelectionListener implements SelectionListener
	{
		public TreeSelectionListener() {
		}

		public void widgetSelected(SelectionEvent e) {
			TreeItem ti= (TreeItem) e.item;
			Object data= ti.getData();
			// data is assumed to be an instance of an AST node in some
			// language-specific type
			
			if (data == null) {
				System.out.println("Wiget selected for outline item with no node; returning");
				return;
			}
	
			// It's actually possible (if not probable) that the outlining service will be invoked
			// by the editor so soon in the initialization of a workspace that the following composite
			// call will fail (e.g., due to null values returned at various points).  However, execution
			// of this method depends on the selection of a wiget, which requires that the ActivePage,
			// ActiveWorkbenchWindow, etc. be established.  So for the outliner, this sequence of calls
			// is not a problem in practice.
			IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			AbstractTextEditor textEditor= (AbstractTextEditor) activeEditor;

			if (!(textEditor instanceof UniversalEditor)) {
				System.err.println("OutlinerBase.TreeSelectionListener.wigetSelected(..):  text editor is not a UniversalEditor!  Don't know what to do with it!");
				return;
			}
			UniversalEditor uE = (UniversalEditor) textEditor;

			IASTNodeLocator nodeLocator = uE.getParseController().getNodeLocator();
			int startOffset = 0;
			int endOffset = 0;
		
			try {
				// The methods of nodeLocator typically take an object and cast it
				// to an ASTNode type without any concern for what it actually is,
				// so try that here and catch any exception that is thrown
				startOffset = nodeLocator.getStartOffset(data);
				endOffset = nodeLocator.getEndOffset(data);
			} catch (ClassCastException x) {
				System.err.println("OutlinerBase.TreeSelectionListener.wigetSelected:  " +
					"ClassCastException trying to treat event data as AST node type");
				return;
			}

			textEditor.selectAndReveal(startOffset, endOffset-startOffset+1);
		}
		
		public void widgetDefaultSelected(SelectionEvent e) { }
	}
	
	
	public void setTree(Tree tree) {
		this.tree = tree;
		this.tree.addSelectionListener(new TreeSelectionListener());
	}
 
	
	
	/*
	 * Fields and methods for representing and manipulating
	 * the stack of outline items
	 */

	private Stack fItemStack = new Stack();
	
	public void pushTopItem(String itemName, Object node) {
		fItemStack.push(createTopItem(itemName, node));
	}

	public void pushSubItem(String itemName, Object node) {
		fItemStack.push(createSubItem(itemName, node));
	}

	public void popSubItem() {
		fItemStack.pop();
	}
  
	public void addSubItem(String label, Object node) {
  		createSubItem(label, node);
	}
	
	
	/*
	 * Following are two pairs of methods, one for creating new top-level
	 * items, and one for creating new sub items, in the outline tree.
	 * The first method of each pair has been hoisted from the outline
	 * template and modified to reference a default outline image (to break
	 * the dependence on a language-specific image).  The second method of
	 * each pair is analogous to the first but takes an IOutlineImage
	 * to allow language-specific images to be provided.
	 * 
	 * (Note:  IOutlineImage and DefaultOutlineImage were newly created
	 * to enable this refactoring.)
	 */
	
	public TreeItem createTopItem(String label, Object n) {
			TreeItem treeItem= new TreeItem(tree, SWT.NONE);
			treeItem.setText(label);
			treeItem.setImage(DefaultOutlineImage.getDefaultOutlineImage().getOutlineItemImage());
			if (n != null)
			  treeItem.setData(n);
			return treeItem;
	}
	
	public TreeItem createTopItem(String label, Object n, IOutlineImage image) {
			TreeItem treeItem= new TreeItem(tree, SWT.NONE);
			treeItem.setText(label);
			treeItem.setImage(image.getOutlineItemImage());
			if (n != null)
			  treeItem.setData(n);
			return treeItem;
	}
	
	
	public TreeItem createSubItem(String label, Object n) {
			TreeItem treeItem= new TreeItem((TreeItem) fItemStack.peek(), SWT.NONE);
			treeItem.setText(label);
			  if (n != null)
				  treeItem.setData(n);
			treeItem.setImage(DefaultOutlineImage.getDefaultOutlineImage().getOutlineItemImage());
			return treeItem;
	}
	
	
	public TreeItem createSubItem(String label, Object n, IOutlineImage image) {
			TreeItem treeItem= new TreeItem((TreeItem) fItemStack.peek(), SWT.NONE);
			treeItem.setText(label);
			  if (n != null)
				  treeItem.setData(n);
			treeItem.setImage(image.getOutlineItemImage());
			return treeItem;
	}

  
	
	/**
	 * Create the outline tree representing the AST associated with a given
	 * parse controller.  There are two issues of particular concern in the
	 * design of this method:  observing the appropriate UI protocol for
	 * redrawing the tree, and filtering out unwanted invocations that
	 * reflect events that should probably not trigger a redrawing of the tree.
	 * 
	 * Regarding the protocol for redrawing the tree, it seems that calls to
	 * tree.setRedraw(true) must be balanced by calls to tree.setRedraw(false).
	 * This is because of logic in org.eclipse.swt.widgets.Control, the parent
	 * class of org.eclipse.swt.widgets.TreeItem.  If there are excessive calls
	 * to tree.setRedraw(true) then the outline view can become "stuck" on a
	 * particular file; that is, when a new editor is opened or brought to
	 * the foreground, the outline view continues to show the outline for a
	 * previous file.  The implementation provided here generally assures,
	 * insofar as possible, that the calls to tree.setRedraw(true) and to
	 * tree.setRedraw(false) are balanced, even in the face of exceptions.
	 * If the logic of this method is modified, care should be taken to assure
	 * that this balance is maintained.
	 * 
	 * Regarding unwanted invocations, this method may be called even when
	 * the AST represented has not changed and thus when redrawing of
	 * the outline may be unnecessary.  Unnecessary redrawing of the outline
	 * is probably a negligable inefficiency in most cases, but redrawing has
	 * the effect of collapsing an expanded outline, which can be annoying
	 * for users.  Folding of the source text in an editor is one case in which
	 * this method is called when there is no change to the underlying AST.
	 * The implementation provided here includes a test for "significantChange"
	 * and avoids redrawing the outline when no significant change has occurred.
	 * The provided version of significantChange(..) tracks changes to the
	 * whole tree, the number of tokens, and the text associated with individual
	 * tokens.  Users who want to use an alternative definition of "significant
	 * change" should reimplement the change-detection strategy type.
	 * 
	 * @param	controller	A parse controller from which an AST can be obtained
	 * 						to serve as the basis of an outline presentation
	 * 	
	 * @param	offset		Currently ignored; kept for backward compatibility
	 * 
	 * @return void		Has no return but has the effect of (re)drawing
	 * 					an outline tree based on the AST obtained from the
	 * 					given parse controller, if possible.
	 * 
	 *   					Has no effect under a variety of problematic
	 * 					conditions:  given parse controller is null, outline
	 * 					tree is null, parse controller has errors, or parse
	 * 					controller's current AST is null.  Also has no
	 * 					effect if there is no "significant change" from the
	 * 					the AST on the previous invocation to the AST on this
	 * 					invocation.
	 * 
	 * @exception			Throws none; traps all exceptions and reports an
	 * 						error.
	 */
	public void createOutlinePresentation(IParseController controller, int offset)
	{
		if (controller == null || tree == null) return;
		if (controller.hasErrors()) return;
		if (!significantChange(controller)) return;
		
		boolean redrawSetFalse = false;
			try {
				Object root= controller.getCurrentAst();
				if (root == null) return;
							
				tree.setRedraw(false);
				redrawSetFalse = true;
				tree.removeAll();
				fItemStack.clear();
				sendVisitorToAST(root);
			} catch (Throwable e) {
			  ErrorHandler.reportError("Exception generating outlinel", e);
			} finally {
			  if (redrawSetFalse) {
			  	tree.setRedraw(true);
				}
			}
	}
  
	

	/*
     * To hold in a generic way any data that the method "significantChange(..)"
     * may require from one invocation to the next.
     */
    private Object[] previous = null;
  
	/**
	 * This method is intended to detect whether there has been a significant
	 * change in the AST represented by the given controller (presumably such
	 * that the outline should be redrawn).
	 * 
	 * The default implementation here simply returns true; this method should
	 * be overridden in subclasses to implement language-specific tests.
	 * 
	 * @param controller	Provides access to the AST represented in the outline
	 * @return			Whether there has been a significant change in the
	 * 					AST (by default TRUE)
	 */
	protected boolean significantChange(IParseController controller) {
		// TODO:  Override this in the language-specific outliner,
		// adopting a language-specific change-detection strategy as
		// appropriate	
		//return true;
		
	   	boolean previousWasNull = previous == null;
    	boolean result = false;
    	
    	// Check for previous values being null (as in uninitialized)
    	if (previousWasNull) {
    		// create and initialize previous
    		previous = new Object[3];
    		for (int i = 0; i < previous.length; i++) {
    			previous[i] = null;
    		}
    		
    		// check for current and previous controllers both null	
    		if (controller == null) {
    			return false;
    		}
    	}
    	
    	// If here then had some previous values (although these
    	// could individually be null); is current controller null?
    	if (controller == null) {
    		for (int i = 0; i < previous.length; i++) {
    			if (previous[i] == null) continue;	// not changed
    			result = true;						// changed
    			previous[i] = null;					// null now
    		}
    		return result;
    	}
    	
    	// If here then had some previous values and have some current
    	// values; these need to be compared
    	// (for simplicity assume that current values are not null)
    	
    	// Get current values for comparison to previous
    	ArrayList tokens = controller.getParser().getParseStream().getTokens();
    	char[] chars = controller.getLexer().getLexStream().getInputChars();
    	
    	// Get previous values for comparison to current
    	IParseController previousController = (IParseController) previous[0];
    	ArrayList previousTokens = (ArrayList) previous[1];
    	char[] previousChars = (char[]) previous[2];
    	
    	// Update previous values to current values in any case (now that
    	// we've saved previous in local fields)
		previous[0] = controller;	
		previous[1] = tokens;
		previous[2] = chars;
    	
    	// Compare current and previous values; return true if different
		
		// Are the whole trees different?  (Assume so if controllers differ)
    	if (previousController != controller) return true;
    	
    	// Are the sizes of the trees different? 
    	if (previousTokens.size() != tokens.size()) {
    		return true;
    	}
    	
    	// Are any of the individual tokens different?
    	for (int i = 0; i < previousTokens.size()-1; i++) {
    		IToken previousToken = (IToken)previousTokens.get(i);
    		IToken token = (IToken)tokens.get(i);
    		if (previousToken.getKind() != token.getKind()) {
    			//System.out.println("Previous and current tokens differ at token # = " + i);
    			return true;
    		}
    		int previousStart = previousToken.getStartOffset();
    		int previousEnd = previousToken.getEndOffset();
    		int start = token.getStartOffset();
    		int end = token.getEndOffset();
    		if ((previousEnd - previousStart) != (end - start)) {
				// System.out.println("Previous and current tokens have different extents at token # = " + i);
				return true;
    		}
    		for (int j = 0; j < (previousEnd - previousStart + 1); j++) {
    			if ((previousChars.length != 0) && previousChars[previousStart+j] != chars[start+j]) {
    				// System.out.println("Previous and current tokens have different characters at token # = " + i +
    				//		", character # = " + j);
    				return true;
    			}
    		}
    	}
    	
    	// No significant differences found
    	return false;
		
	}	
  
	/**
	 * Intended to treat the given Object as the root of an AST to which an
	 * outline visitor should be sent.  Both the AST node and the implicit visitor
	 * are presumed to be language-specific types that will be known in a subclass
	 * of this class.
	 * 
	 * @param node		Presumably the root of an AST of some language-specific
	 * 					type
	 */
	protected abstract void sendVisitorToAST(Object node);

}