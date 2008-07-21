package org.eclipse.imp.editor;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;

public class EditorInputUtils {
    /**
     * @return the IPath corresponding to the given input, or null if none
     */
    public static IPath getPath(IEditorInput editorInput) {
        IPath path= null;

        if (editorInput instanceof IFileEditorInput) {
            IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
            path= fileEditorInput.getFile().getProjectRelativePath();
        } else if (editorInput instanceof IPathEditorInput) {
            IPathEditorInput pathInput= (IPathEditorInput) editorInput;
            path= pathInput.getPath();
        } else if (editorInput instanceof IStorageEditorInput) {
            IStorageEditorInput storageEditorInput= (IStorageEditorInput) editorInput;
            try {
                path= storageEditorInput.getStorage().getFullPath(); // can be null
            } catch (CoreException e) {
                // do nothing; return null;
            }
        } else if (editorInput instanceof FileStoreEditorInput) {
            FileStoreEditorInput fileStoreEditorInput= (FileStoreEditorInput) editorInput;
            path= new Path(fileStoreEditorInput.getURI().getPath());
        }
        return path;
    }

    /**
     * @return the IFile corresponding to the given input, or null if none
     */
    public static IFile getFile(IEditorInput editorInput) {
        IFile file= null;

        if (editorInput instanceof IFileEditorInput) {
            IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
            file= fileEditorInput.getFile();
        } else if (editorInput instanceof IPathEditorInput) {
            IPathEditorInput pathInput= (IPathEditorInput) editorInput;
            IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();

            if (wsRoot.getLocation().isPrefixOf(pathInput.getPath())) {
                file= ResourcesPlugin.getWorkspace().getRoot().getFile(pathInput.getPath());
            } else {
                // Can't get an IFile for an arbitrary file on the file system; return null
            }
        } else if (editorInput instanceof IStorageEditorInput) {
            IStorageEditorInput storageEditorInput= (IStorageEditorInput) editorInput;
            file= null; // Can't get an IFile for an arbitrary IStorageEditorInput
        } else if (editorInput instanceof FileStoreEditorInput) {
            IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
            FileStoreEditorInput fileStoreEditorInput= (FileStoreEditorInput) editorInput;
            URI uri= fileStoreEditorInput.getURI();
            String path= uri.getPath();
            // Bug 526: uri.getHost() can be null for a local file URL
            if (uri.getScheme().equals("file") && (uri.getHost() == null || uri.getHost().equals("localhost")) && path.startsWith(wsRoot.getLocation().toOSString())) {
                file= wsRoot.getFile(new Path(path));
            }
        }
        return file;
    }

    /**
     * @return the name extension (e.g., "java" or "cpp") corresponding to this
     * input, if known, or the empty string if none. Does not include a leading
     * ".".
     */
    public static String getNameExtension(IEditorInput editorInput) {
        return getPath(editorInput).getFileExtension();
    }
}
