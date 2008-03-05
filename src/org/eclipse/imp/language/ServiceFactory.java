package org.eclipse.imp.language;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.imp.editor.OutlineContentProviderBase;
import org.eclipse.imp.editor.OutlineLabelProvider.IElementImageProvider;
import org.eclipse.imp.indexing.IndexContributorBase;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.imp.services.IASTAdapter;
import org.eclipse.imp.services.IAnnotationHover;
import org.eclipse.imp.services.IAutoEditStrategy;
import org.eclipse.imp.services.IContentProposer;
import org.eclipse.imp.services.IDocumentationProvider;
import org.eclipse.imp.services.IFoldingUpdater;
import org.eclipse.imp.services.IHoverHelper;
import org.eclipse.imp.services.ILabelProvider;
import org.eclipse.imp.services.ILanguageActionsContributor;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.imp.services.IOccurrenceMarker;
import org.eclipse.imp.services.IOutliner;
import org.eclipse.imp.services.IRefactoringContributor;
import org.eclipse.imp.services.IReferenceResolver;
import org.eclipse.imp.services.ISourceFormatter;
import org.eclipse.imp.services.ISourceHyperlinkDetector;
import org.eclipse.imp.services.ITokenColorer;
import org.eclipse.imp.services.base.TreeModelBuilderBase;
import org.eclipse.imp.utils.ExtensionFactory;
import org.eclipse.imp.utils.ExtensionException;

/**
 * This class stores language services. IMP services are configured with
 * language specific extension points. This registry provides implementations
 * for them. It finds the implementations by looking for Eclipse extensions for
 * IMP's extension points.
 * 
 * If IMP is extended with a new kind of language service, this class must be
 * extended.
 * 
 * The getter methods of this class return 'null' when a service does not exist
 * (i.e. an extension has not been provided yet)
 * 
 * The getter methods of this class will throw unchecked exceptions when the
 * extension implementations are not well formed.
 * 
 * The getter methods only load the extension implementations the first time
 * somebody asks for them. After that they are cached in the registry. This lazy
 * behavior is necessary to optimize the startup time of Eclipse.
 * 
 * @author jurgenv
 * 
 */
public class ServiceFactory {
    private static ServiceFactory sInstance;

    String AUTO_EDIT_SERVICE = "autoEditStrategy";

    String ANNOTATION_HOVER_SERVICE = "annotationHover";

    String AST_ADAPTER_SERVICE = "astAdapter";

    String CONTENT_PROPOSER_SERVICE = "contentProposer";

    String DOCUMENTATION_PROVIDER_SERVICE = "documentationProvider";

    String EDITOR_ACTION_SERVICE = "editorActionContributions";

    String FOLDING_SERVICE = "foldingUpdater";

    String FORMATTER_SERVICE = "formatter";

    String HOVER_HELPER_SERVICE = "hoverHelper";

    String HYPERLINK_SERVICE = "hyperLink";

    String IMAGE_DECORATOR_SERVICE = "imageDecorator";

    String INDEX_CONTRIBUTOR_SERVICE = "indexContributor";

    String LABEL_PROVIDER_SERVICE = "labelProvider";

    String LISTENER_SERVICE = "modelListener";

    String MODEL_BUILDER_SERVICE = "modelTreeBuilder";

    String OCCURRENCE_MARKER = "markOccurrences";

    String OUTLINE_CONTENT_PROVIDER_SERVICE = "outlineContentProvider";

    String OUTLINER_SERVICE = "outliner";

    String PARSER_SERVICE = "parser";

    String PREFERENCES_SERVICE = "preferencesDialog";

    String PREFERENCES_SPECIFICATION = "preferencesSpecification";

    String REFACTORING_CONTRIBUTIONS_SERVICE = "refactoringContributions";

    String REFERENCE_RESOLVER_SERVICE = "referenceResolvers";

    String SYNTAX_PROPS = "syntaxProps";

    String TOKEN_COLORER_SERVICE = "tokenColorer";

    String VIEWER_FILTER_SERVICE = "viewerFilter";

    protected ServiceFactory() {
    }

    /**
     * Returns the {@link ServiceFactory}. IMP services are configured with
     * language specific extension points. This registry provides the
     * implementations for them. This class finds these implementations via
     * Eclipse's extension point mechanism.
     * 
     * @return
     */
    public static ServiceFactory getInstance() {
        if (sInstance == null) {
            sInstance = new ServiceFactory();
        }
        return sInstance;
    }

    public IContentProposer getContentProposer(Language lang) {
        try {
            return (IContentProposer) loadService(lang,
                    CONTENT_PROPOSER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of CONTENT_PROPOSER_SERVICE does not implement IContentProposer",
                    e);
            return null;
        }
    }

    public IHoverHelper getHoverHelper(Language lang) {
        try {
            return (IHoverHelper) loadService(lang, HOVER_HELPER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of HOVER_HELPER_SERVICE does not implement IHoverHelper",
                    e);
            return null;
        }
    }

    public ITokenColorer getTokenColorer(Language lang) {
        try {
            return (ITokenColorer) loadService(lang, TOKEN_COLORER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of TOKEN_COLORER_SERVICE does not implement ITokenColorer",
                    e);
            return null;
        }
    }

    public IndexContributorBase getIndexContributor(Language lang) {
        try {
            return (IndexContributorBase) loadService(lang,
                    INDEX_CONTRIBUTOR_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of INDEX_CONTRIBUTOR_SERVICE does not implement IndexContributorBase",
                    e);
            return null;
        }
    }

    public IParseController getParseController(Language lang) {
        try {
            return (IParseController) loadService(lang, PARSER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of PARSER_SERVICE does not implement IParseController",
                    e);
            return null;
        }
    }

    public TreeModelBuilderBase getTreeModelBuilder(Language lang) {
        try {
            return (TreeModelBuilderBase) loadService(lang,
                    MODEL_BUILDER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of MODEL_BUILDER_SERVICE does not implement TreeModelBuilderBase",
                    e);
            return null;
        }
    }

    public IModelListener getModelListener(Language lang) {
        try {
            return (IModelListener) loadService(lang, LISTENER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of LISTENER_SERVICE does not implement IModelListener",
                    e);
            return null;
        }
    }

    public IAutoEditStrategy getAutoEditStrategy(Language lang) {
        try {
            return (IAutoEditStrategy) loadService(lang, AUTO_EDIT_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of AUTO_EDIT_SERVICE does not implement IAutoEditStrategy",
                    e);
            return null;
        }
    }

    public IFoldingUpdater getFoldingUpdater(Language lang) {
        try {
            return (IFoldingUpdater) loadService(lang, FOLDING_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of FOLDING_SERVICE does not implement IFoldingUpdater",
                    e);
            return null;
        }
    }

    public IAnnotationHover getAnnotationHover(Language lang) {
        try {
            return (IAnnotationHover) loadService(lang,
                    ANNOTATION_HOVER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of ANNOTATION_HOVER_SERVICE does not implement IAnnotationHover",
                    e);
            return null;
        }
    }

    public ISourceFormatter getSourceFormatter(Language lang) {
        try {
            return (ISourceFormatter) loadService(lang, FORMATTER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of FORMATTER_SERVICE does not implement ISourceFormatter",
                    e);
            return null;
        }
    }

    public ISourceHyperlinkDetector getSourceHyperlinkDetector(Language lang) {
        try {
            return (ISourceHyperlinkDetector) loadService(lang,
                    HYPERLINK_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of HYPERLINK_SERVICE does not implement ISourceHyperlinkDetector",
                    e);
            return null;
        }
    }

    public ILabelProvider getLabelProvider(Language lang) {
        try {
            return (ILabelProvider) loadService(lang, LABEL_PROVIDER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of LABEL_PROVIDER_SERVICE does not implement ILabelProvider",
                    e);
            return null;
        }
    }

    public OutlineContentProviderBase getOutlineContentProvider(Language lang) {
        try {
            return (OutlineContentProviderBase) loadService(lang,
                    OUTLINE_CONTENT_PROVIDER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of OUTLINE_CONTENT_PROVIDER_SERVICE does not implement OutlineContentProviderBase",
                    e);
            return null;
        }
    }

    public Set<IRefactoringContributor> getRefactoringContributors(Language lang) {
        try {
            Set<ILanguageService> services = loadServices(lang,
                    REFACTORING_CONTRIBUTIONS_SERVICE);
            Set<IRefactoringContributor> refactoringContribs = new HashSet<IRefactoringContributor>();

            for (ILanguageService s : services) {
                refactoringContribs.add((IRefactoringContributor) s);
            }

            return refactoringContribs;
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of SERVICE does not implement Set<ILanguageSerivice>",
                    e);
            return null;
        }
    }

    public IReferenceResolver getReferenceResolver(Language lang) {
        try {
            return (IReferenceResolver) loadService(lang,
                    REFERENCE_RESOLVER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of REFERENCE_RESOLVER_SERVICE does not implement IReferenceResolver",
                    e);
            return null;
        }
    }

    public Set<ILanguageActionsContributor> getLanguageActionsContributors(
            Language lang) {
        try {
            Set<ILanguageService> services = loadServices(lang,
                    EDITOR_ACTION_SERVICE);

            Set<ILanguageActionsContributor> actionContributors = new HashSet<ILanguageActionsContributor>();

            for (ILanguageService s : services) {
                actionContributors.add((ILanguageActionsContributor) s);
            }

            return actionContributors;
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of EDITOR_ACTION_SERVICE does not implement ILanguageActionConstributor",
                    e);
            return null;
        }
    }

    public IDocumentationProvider getDocumentationProvider(Language lang) {
        try {
            return (IDocumentationProvider) loadService(lang,
                    DOCUMENTATION_PROVIDER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of DOCUMENTATION_PROVIDER_SERVICE does not implement IDocumentationProvider",
                    e);
            return null;
        }
    }

    public IOccurrenceMarker getOccurrenceMarker(Language lang) {
        try {
            return (IOccurrenceMarker) loadService(lang, OCCURRENCE_MARKER);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of OCCURRENCE_MARKER does not implement IOccurrenceMarker",
                    e);
            return null;
        }
    }

    public ILanguageSyntaxProperties getSyntaxProperties(Language lang) {
        try {
            return (ILanguageSyntaxProperties) loadService(lang, SYNTAX_PROPS);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of SYNTAX_PROPS does not implement ILanguageSyntaxProperties",
                    e);
            return null;
        }
    }

    public IElementImageProvider getElementImageProvider(Language lang) {
        try {
            return (IElementImageProvider) loadService(lang,
                    IMAGE_DECORATOR_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of IMAGE_DECORATOR_SERVICE does not implement IElementImageProvider",
                    e);
            return null;
        }
    }

    public IOutliner getOutliner(Language lang) {
        try {
            return (IOutliner) loadService(lang, OUTLINER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of OLD_OUTLINER_SERVICE does not implement IOutliner",
                    e);
            return null;
        }
    }

    public IASTAdapter getASTAdapter(Language lang) {
        try {
            return (IASTAdapter) loadService(lang, AST_ADAPTER_SERVICE);
        } catch (ClassCastException e) {
            RuntimePlugin.getInstance().logException(
                    "Alleged implementation of AST_ADAPTER_SERVICE does not implement IASTAdapter",
                    e);
            return null;
        }
    }

    private ILanguageService createExtension(Language lang, String id) {
        try {
            return ExtensionFactory.createServiceExtension(lang, id);
        } catch (ExtensionException e) {
            RuntimePlugin.getInstance().logException(
                    "Failed to create extension: " + id, e);
            return null;
        }
    }

    private Set<ILanguageService> createExtensions(Language lang, String id) {
        try {
          return ExtensionFactory.createServiceExtensionSet(lang, id);
        } 
        catch (ExtensionException e) {
            RuntimePlugin.getInstance().logException(
                    "Failed to create set of extensions for: " + id, e);
            return new HashSet<ILanguageService>();
        }
    }

    private Set<ILanguageService> loadServices(Language lang, String serviceId) {
        return createExtensions(lang, serviceId);
    }

    private ILanguageService loadService(Language lang, String name) {
        if (lang == null) {
            RuntimePlugin.getInstance().writeErrorMsg("No service for null language");
            return null;
        }
        return createExtension(lang, name);
    }
}