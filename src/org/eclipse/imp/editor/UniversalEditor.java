package org.eclipse.uide.editor;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2004  All Rights Reserved
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import lpg.lpgjavaruntime.IToken;
import lpg.lpgjavaruntime.PrsStream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.uide.core.ErrorHandler;
import org.eclipse.uide.core.Language;
import org.eclipse.uide.core.LanguageRegistry;
import org.eclipse.uide.defaults.DefaultAnnotationHover;
import org.eclipse.uide.internal.editor.FoldingController;
import org.eclipse.uide.internal.editor.FormattingController;
import org.eclipse.uide.internal.editor.OutlineController;
import org.eclipse.uide.internal.editor.PresentationController;
import org.eclipse.uide.internal.editor.SourceHyperlinkController;
import org.eclipse.uide.parser.IModelListener;
import org.eclipse.uide.parser.IParseController;
import org.eclipse.uide.runtime.RuntimePlugin;
import org.eclipse.uide.utils.ExtensionPointFactory;

/**
 * An Eclipse editor. This editor is not enhanced using API. Instead, we publish extension points for outline, content assist, hover help, etc.
 * 
 * Credits go to Martin Kersten and Bob Foster for guiding the good parts of this design. Sole responsiblity for the bad parts rest with Chris Laffra.
 * 
 * @author Chris Laffra
 * @author Robert M. Fuhrer
 */
public class UniversalEditor extends TextEditor implements IASTFindReplaceTarget {
    public static final String EDITOR_ID= RuntimePlugin.UIDE_RUNTIME + ".safariEditor";

    public static final String PARSE_ANNOTATION_TYPE= "org.eclipse.uide.editor.parseAnnotation";

    protected Language fLanguage;

    protected ParserScheduler fParserScheduler;

    protected HoverHelpController fHoverHelpController;

    protected OutlineController fOutlineController;

    protected PresentationController fPresentationController;

    protected CompletionProcessor fCompletionProcessor;

    protected SourceHyperlinkController fHyperLinkController;

    protected ISourceHyperlinkDetector fHyperLinkDetector;

    protected IAutoEditStrategy fAutoEditStrategy;

    private IFoldingUpdater fFoldingUpdater;

    private ProjectionAnnotationModel fAnnotationModel;

    public ISourceFormatter fFormattingStrategy;

    private FormattingController fFormattingController;

    private static final String BUNDLE_FOR_CONSTRUCTED_KEYS= "org.eclipse.uide.editor.messages";//$NON-NLS-1$

    static ResourceBundle fgBundleForConstructedKeys= ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

    public UniversalEditor() {
	setSourceViewerConfiguration(new Configuration());
	configureInsertMode(SMART_INSERT, true);
	setInsertMode(SMART_INSERT);
    }

    public Object getAdapter(Class required) {
	if (IContentOutlinePage.class.equals(required)) {
	    return fOutlineController;
	}
	if (IToggleBreakpointsTarget.class.equals(required)) {
	    return new ToggleBreakpointsAdapter();
	}
	return super.getAdapter(required);
    }

    protected void createActions() {
	super.createActions();

        Action action= new ContentAssistAction(ResourceBundle.getBundle("org.eclipse.uide.editor.messages"), "ContentAssistProposal.", this);
	action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
	setAction("ContentAssistProposal", action);
	markAsStateDependentAction("ContentAssistProposal", true);

        action= new TextOperationAction(ResourceBundle.getBundle("org.eclipse.uide.editor.messages"), "Format.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
        action.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
        setAction("Format", action); //$NON-NLS-1$
        markAsStateDependentAction("Format", true); //$NON-NLS-1$
        markAsSelectionDependentAction("Format", true); //$NON-NLS-1$
        PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.FORMAT_ACTION);
    }

    /**
     * Sets the given message as error message to this editor's status line.
     *
     * @param msg message to be set
     */
    protected void setStatusLineErrorMessage(String msg) {
	IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
	if (statusLine != null)
	    statusLine.setMessage(true, msg, null);
    }

    /**
     * Sets the given message as message to this editor's status line.
     *
     * @param msg message to be set
     * @since 3.0
     */
    protected void setStatusLineMessage(String msg) {
	IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
	if (statusLine != null)
	    statusLine.setMessage(false, msg, null);
    }

    /**
     * Jumps to the next enabled annotation according to the given direction.
     * An annotation type is enabled if it is configured to be in the
     * Next/Previous tool bar drop down menu and if it is checked.
     *
     * @param forward <code>true</code> if search direction is forward, <code>false</code> if backward
     */
    public void gotoAnnotation(boolean forward) {
	ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
	Position position= new Position(0, 0);

	if (false /* delayed - see bug 18316 */) {
	    getNextAnnotation(selection.getOffset(), selection.getLength(), forward, position);
	    selectAndReveal(position.getOffset(), position.getLength());
	} else /* no delay - see bug 18316 */{
	    Annotation annotation= getNextAnnotation(selection.getOffset(), selection.getLength(), forward, position);

	    setStatusLineErrorMessage(null);
	    setStatusLineMessage(null);
	    if (annotation != null) {
		updateAnnotationViews(annotation);
		selectAndReveal(position.getOffset(), position.getLength());
		setStatusLineMessage(annotation.getText());
	    }
	}
    }

    /**
     * Returns the annotation closest to the given range respecting the given
     * direction. If an annotation is found, the annotations current position
     * is copied into the provided annotation position.
     *
     * @param offset the region offset
     * @param length the region length
     * @param forward <code>true</code> for forwards, <code>false</code> for backward
     * @param annotationPosition the position of the found annotation
     * @return the found annotation
     */
    private Annotation getNextAnnotation(final int offset, final int length, boolean forward, Position annotationPosition) {
	Annotation nextAnnotation= null;
	Position nextAnnotationPosition= null;
	Annotation containingAnnotation= null;
	Position containingAnnotationPosition= null;
	boolean currentAnnotation= false;

	IDocument document= getDocumentProvider().getDocument(getEditorInput());
	int endOfDocument= document.getLength();
	int distance= Integer.MAX_VALUE;

	IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());

	for(Iterator e= model.getAnnotationIterator(); e.hasNext(); ) {
	    Annotation a= (Annotation) e.next();
	    //	    if ((a instanceof IJavaAnnotation) && ((IJavaAnnotation) a).hasOverlay() || !isNavigationTarget(a))
	    //		continue;
	    // TODO RMF 4/19/2006 - Need more accurate logic here for filtering annotations
	    if (!(a instanceof MarkerAnnotation) && !a.getType().equals(PARSE_ANNOTATION_TYPE))
		continue;

	    Position p= model.getPosition(a);
	    if (p == null)
		continue;

	    if (forward && p.offset == offset || !forward && p.offset + p.getLength() == offset + length) {// || p.includes(offset)) {
		if (containingAnnotation == null
			|| (forward && p.length >= containingAnnotationPosition.length || !forward
				&& p.length >= containingAnnotationPosition.length)) {
		    containingAnnotation= a;
		    containingAnnotationPosition= p;
		    currentAnnotation= p.length == length;
		}
	    } else {
		int currentDistance= 0;

		if (forward) {
		    currentDistance= p.getOffset() - offset;
		    if (currentDistance < 0)
			currentDistance= endOfDocument + currentDistance;

		    if (currentDistance < distance || currentDistance == distance && p.length < nextAnnotationPosition.length) {
			distance= currentDistance;
			nextAnnotation= a;
			nextAnnotationPosition= p;
		    }
		} else {
		    currentDistance= offset + length - (p.getOffset() + p.length);
		    if (currentDistance < 0)
			currentDistance= endOfDocument + currentDistance;

		    if (currentDistance < distance || currentDistance == distance && p.length < nextAnnotationPosition.length) {
			distance= currentDistance;
			nextAnnotation= a;
			nextAnnotationPosition= p;
		    }
		}
	    }
	}
	if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
	    annotationPosition.setOffset(containingAnnotationPosition.getOffset());
	    annotationPosition.setLength(containingAnnotationPosition.getLength());
	    return containingAnnotation;
	}
	if (nextAnnotationPosition != null) {
	    annotationPosition.setOffset(nextAnnotationPosition.getOffset());
	    annotationPosition.setLength(nextAnnotationPosition.getLength());
	}

	return nextAnnotation;
    }

    /**
     * Updates the annotation views that show the given annotation.
     *
     * @param annotation the annotation
     */
    private void updateAnnotationViews(Annotation annotation) {
	IMarker marker= null;
	if (annotation instanceof MarkerAnnotation)
	    marker= ((MarkerAnnotation) annotation).getMarker();
	else
	//        if (annotation instanceof IJavaAnnotation) {
	//	    Iterator e= ((IJavaAnnotation) annotation).getOverlaidIterator();
	//	    if (e != null) {
	//		while (e.hasNext()) {
	//		    Object o= e.next();
	//		    if (o instanceof MarkerAnnotation) {
	//			marker= ((MarkerAnnotation) o).getMarker();
	//			break;
	//		    }
	//		}
	//	    }
	//	}

	if (marker != null /*&& !marker.equals(fLastMarkerTarget)*/) {
	    try {
		boolean isProblem= marker.isSubtypeOf(IMarker.PROBLEM);
		IWorkbenchPage page= getSite().getPage();
		IViewPart view= page.findView(isProblem ? IPageLayout.ID_PROBLEM_VIEW : IPageLayout.ID_TASK_LIST); //$NON-NLS-1$  //$NON-NLS-2$
		if (view != null) {
		    Method method= view.getClass().getMethod(
			    "setSelection", new Class[] { IStructuredSelection.class, boolean.class }); //$NON-NLS-1$
		    method.invoke(view, new Object[] { new StructuredSelection(marker), Boolean.TRUE });
		}
	    } catch (CoreException x) {
	    } catch (NoSuchMethodException x) {
	    } catch (IllegalAccessException x) {
	    } catch (InvocationTargetException x) {
	    }
	    // ignore exceptions, don't update any of the lists, just set status line
	}
    }

    public void createPartControl(Composite parent) {
	fLanguage= LanguageRegistry.findLanguage(getEditorInput());

	// Create language service extensions now, for any services that could
	// get invoked via super.createPartControl().
	if (fLanguage != null) {
	    fHyperLinkDetector= (ISourceHyperlinkDetector) createExtensionPoint("hyperLink");
	    if (fHyperLinkDetector != null)
	    	fHyperLinkController= new SourceHyperlinkController(fHyperLinkDetector);
	    fFoldingUpdater= (IFoldingUpdater) createExtensionPoint("foldingUpdater");
	    fFormattingStrategy= (ISourceFormatter) createExtensionPoint("formatter");
	    fFormattingController= new FormattingController(fFormattingStrategy);
	}

	super.createPartControl(parent);

	if (fLanguage != null) {
	    try {
		fOutlineController= new OutlineController(this);
		fPresentationController= new PresentationController(getSourceViewer());
		fPresentationController.damage(0, getSourceViewer().getDocument().getLength());
		fParserScheduler= new ParserScheduler("Universal Editor Parser");
		fFormattingController.setParseController(fParserScheduler.parseController);

		if (fFoldingUpdater != null) {        
		    ProjectionViewer viewer= (ProjectionViewer) getSourceViewer();
		    ProjectionSupport projectionSupport= new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());

		    projectionSupport.install();
		    viewer.doOperation(ProjectionViewer.TOGGLE);
		    fAnnotationModel= viewer.getProjectionAnnotationModel();
		    fParserScheduler.addModelListener(new FoldingController(fAnnotationModel, fFoldingUpdater));
		}

		fOutlineController.setLanguage(fLanguage);
		fPresentationController.setLanguage(fLanguage);
		fCompletionProcessor.setLanguage(fLanguage);
		fHoverHelpController.setLanguage(fLanguage);

		fParserScheduler.addModelListener(fOutlineController);
		fParserScheduler.addModelListener(fPresentationController);
		fParserScheduler.addModelListener(fCompletionProcessor);
		fParserScheduler.addModelListener(fHoverHelpController);
		
		if (fHyperLinkController != null)
		    fParserScheduler.addModelListener(fHyperLinkController);
		fParserScheduler.run(new NullProgressMonitor());
	    } catch (Exception e) {
		ErrorHandler.reportError("Could not create part", e);
	    }
	}
    }

    /**
     * Override creation of the normal source viewer with one that supports source folding.
     */
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
	if (fFoldingUpdater == null)
	    return super.createSourceViewer(parent, ruler, styles);

	fAnnotationAccess= createAnnotationAccess();
	fOverviewRuler= createOverviewRuler(getSharedColors());

	ISourceViewer viewer= new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
	// ensure decoration support has been created and configured.
	getSourceViewerDecorationSupport(viewer);

	return viewer;
    }

    protected void doSetInput(IEditorInput input) throws CoreException {
	super.doSetInput(input);
	setInsertMode(SMART_INSERT);
    }

    /**
     * Convenience method to create language extensions whose extension point is
     * defined by this plugin.
     * @param extensionPoint the extension point ID of the language service
     * @return the extension implementation
     */
    private Object createExtensionPoint(String extensionPointID) {
	return ExtensionPointFactory.createExtensionPoint(fLanguage, RuntimePlugin.UIDE_RUNTIME, extensionPointID);
    }

    /**
     * Add a Model listener to this editor. Anytime the underlying AST is recomputed, the listener is notified.
     * 
     * @param listener the listener to notify of Model changes
     */
    public void addModelListener(IModelListener listener) {
	fParserScheduler.addModelListener(listener);
    }

    class Configuration extends SourceViewerConfiguration {
	public int getTabWidth(ISourceViewer sourceViewer) {
	    return 8; // TODO should be read from preferences somewhere...
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
	    // BUG Perhaps we shouldn't use a PresentationReconciler; its JavaDoc says it runs in the UI thread!
	    PresentationReconciler reconciler= new PresentationReconciler();
	    reconciler.setRepairer(new PresentationRepairer(), IDocument.DEFAULT_CONTENT_TYPE);
	    return reconciler;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
	    ContentAssistant ca= new ContentAssistant();
	    fCompletionProcessor= new CompletionProcessor();
	    ca.setContentAssistProcessor(fCompletionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
	    ca.setInformationControlCreator(getInformationControlCreator(sourceViewer));
	    return ca;
	}

	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
	    IAnnotationHover hover= null;

	    if (fLanguage != null)
		hover= (IAnnotationHover) createExtensionPoint("annotationHover");
	    if (hover == null)
		hover= new DefaultAnnotationHover();
	    return hover;
	}

	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
	    if (fLanguage != null)
		fAutoEditStrategy= (IAutoEditStrategy) createExtensionPoint("autoEditStrategy");

	    if (fAutoEditStrategy == null)
		fAutoEditStrategy= super.getAutoEditStrategies(sourceViewer, contentType)[0];

	    return new IAutoEditStrategy[] { fAutoEditStrategy };
	}

	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
	    // Disable the content formatter if no language-specific implementation exists.
	    if (fFormattingStrategy == null)
		return null;

	    // BUG For now, assumes only one content type (i.e. one kind of partition)
	    ContentFormatter formatter= new ContentFormatter();

//	    formatter.setDocumentPartitioning("foo");
	    formatter.setFormattingStrategy(fFormattingController, IDocument.DEFAULT_CONTENT_TYPE);
	    return formatter;
	}

	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
	    return super.getDefaultPrefixes(sourceViewer, contentType);
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
	    return super.getDoubleClickStrategy(sourceViewer, contentType);
	}

	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
	    if (fHyperLinkController != null)
		return new IHyperlinkDetector[] { fHyperLinkController };
	    return super.getHyperlinkDetectors(sourceViewer);
	}

	public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
	    return super.getHyperlinkPresenter(sourceViewer);
	}

	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
	    return super.getIndentPrefixes(sourceViewer, contentType);
	}

	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
	    return super.getInformationControlCreator(sourceViewer);
	}

	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
	    return super.getInformationPresenter(sourceViewer);
	}

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
	    return fHoverHelpController= new HoverHelpController();
	}

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
	    return super.getTextHover(sourceViewer, contentType, stateMask);
	}

	public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
	    return super.getUndoManager(sourceViewer);
	}

	public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
	    return super.getOverviewRulerAnnotationHover(sourceViewer);
	}
    }

    class PresentationRepairer implements IPresentationRepairer {
	IDocument fDocument;

	public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
	    // BUG Should we really just ignore the presentation passed in???
	    // JavaDoc says we're responsible for "merging" our changes in...
	    try {
		if (fPresentationController != null) {
		    PrsStream parseStream= fParserScheduler.parseController.getParser().getParseStream();
		    int damagedToken= fParserScheduler.parseController.getTokenIndexAtCharacter(damage.getOffset());

		    // SMS 26 Apr 2006:
		    // (I'd rather see a simple message than a complete stack trace--less alarming for
		    // an occurrence that may not be all that exceptional, and the stack trace is not
		    // really informative, other than telling you that there was a problem here.)
		    // BUT this doesn't seem to catch all exceptions that are thrown in this method
		    // and I can't reliably reproduce the problem that this should catch.  I'm leaving
		    // this here in case it still might work sometimes and as a reminder that some
		    // alternative error handling might be appropriate here.
		    if (damagedToken < 0) {
		    	System.err.println("PresentationRepairer.createPresentation:\n" +
		    			"\tCould not repair damage (damaged token not valid)");
		    	return;
		    }
		    
		    IToken[] adjuncts= parseStream.getFollowingAdjuncts(damagedToken);
		    int endOffset= (adjuncts.length == 0) ? parseStream.getEndOffset(damagedToken)
			    : adjuncts[adjuncts.length - 1].getEndOffset();
		    int length= endOffset - damage.getOffset();

		    fPresentationController.damage(damage.getOffset(),
			    (length > damage.getLength() ? length : damage.getLength()));
		}
		if (fParserScheduler != null) {
		    fParserScheduler.cancel();
		    fParserScheduler.schedule();
		}
	    } catch (Exception e) {
		ErrorHandler.reportError("Could not repair damage ", e);
	    }
	}

	public void setDocument(IDocument document) {
	    fDocument= document;
	}
    }

    private class AnnotationCreator implements IMessageHandler {
	public void handleMessage(int offset, int length, String message) {
	    IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
	    Annotation annotation= new Annotation(PARSE_ANNOTATION_TYPE, false, message);
	    Position pos= new Position(offset, length);

	    model.addAnnotation(annotation, pos);
	}
    }

    private AnnotationCreator fAnnotationCreator= new AnnotationCreator();

    private void removeParserAnnotations() {
	IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());

	for(Iterator i= model.getAnnotationIterator(); i.hasNext(); ) {
	    Annotation a= (Annotation) i.next();

	    if (a.getType().equals(PARSE_ANNOTATION_TYPE))
		model.removeAnnotation(a);
	}
    }

    /**
     * Parsing may take a long time, and is not done inside the UI thread.
     * Therefore, we create a job that is executed in a background thread
     * by the platform's job service.
     */
    // TODO Perhaps this should be driven off of the "IReconcileStrategy" mechanism?
    class ParserScheduler extends Job {
	protected IParseController parseController;

	protected List astListeners= new ArrayList();

	ParserScheduler(String name) {
	    super(name);
	    setSystem(true); // do not show this job in the Progress view
	    parseController= (IParseController) createExtensionPoint("parser");
	}

	protected IStatus run(IProgressMonitor monitor) {
	    try {
		IFileEditorInput fileEditorInput= (IFileEditorInput) getEditorInput();
		IDocument document= getDocumentProvider().getDocument(fileEditorInput);
		String filePath= fileEditorInput.getFile().getProjectRelativePath().toString();

		// Don't need to retrieve the AST; we don't need it.
		// Just make sure the document contents gets parsed once (and only once).
		removeParserAnnotations();
		parseController.initialize(filePath, fileEditorInput.getFile().getProject(), fAnnotationCreator);
		parseController.parse(document.get(), false, monitor);
		if (!monitor.isCanceled())
		    notifyAstListeners(parseController, monitor);
		// else
		//	System.out.println("Bypassed AST listeners (cancelled).");
	    } catch (Exception e) {
	    	ErrorHandler.reportError("Error running parser for " + fLanguage, e);
	    }
	    return Status.OK_STATUS;
	}

	public void addModelListener(IModelListener listener) {
	    astListeners.add(listener);
	}

	public void notifyAstListeners(IParseController parseController, IProgressMonitor monitor) {
	    // Suppress the notification if there's no AST (e.g. due to a parse error)
	    if (parseController != null && parseController.getCurrentAst() != null)
		for(int n= astListeners.size() - 1; n >= 0 && !monitor.isCanceled(); n--) {
		    //((IModelListener) astListeners.get(n)).update(parseController, monitor);
			IModelListener listener = (IModelListener) astListeners.get(n);
			listener.update(parseController, monitor);
		}
	}
    }

    public String getSelectionText() {
	Point sel= getSelection();
        IFileEditorInput fileEditorInput= (IFileEditorInput) getEditorInput();
        IDocument document= getDocumentProvider().getDocument(fileEditorInput);

        try {
	    return document.get(sel.x, sel.y);
	} catch (BadLocationException e) {
	    e.printStackTrace();
	    return "";
	}
    }

    public Point getSelection() {
	ISelection sel= this.getSelectionProvider().getSelection();
	ITextSelection textSel= (ITextSelection) sel;

	return new Point(textSel.getOffset(), textSel.getLength());
    }

    public boolean canPerformFind() {
	return true;
    }

    public IParseController getParseController() {
	return fParserScheduler.parseController;
    }
}
