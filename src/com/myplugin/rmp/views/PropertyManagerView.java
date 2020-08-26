package com.myplugin.rmp.views;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import finalZ.annotation.Module;

import org.eclipse.jdt.launching.JavaRuntime;

public class PropertyManagerView extends ViewPart {
	private TreeViewer viewer;
	private TreeParent invisibleRoot;
	private IJavaProject currentProject;
	private HashMap<String, IJavaProject> javaProjects ;

	class TreeObject implements IAdaptable {

		private String name;
		private TreeParent parent;
		private IResource resouce;

		public TreeObject(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setParent(TreeParent parent) {
			this.parent = parent;
		}

		public TreeParent getParent() {
			return parent;
		}

		public String toString() {
			return getName();
		}

		public Object getAdapter(Class key) {
			return null;
		}
		protected IResource getResouce() {
			return resouce;
		}
		protected void setResouce(IResource resouce) {
			this.resouce = resouce;
		}
	}
	
	class TreeParent extends TreeObject {
		private ArrayList children;
		public TreeParent(String name) {
			super(name);
			children = new ArrayList();
		}

		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}

		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}

		public TreeObject[] getChildren() {
			return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
		}

		public boolean hasChildren() {
			return children.size() > 0;
		}


	}
	
	class ViewContentProvider implements ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot == null)
					initialize(null);

				return getChildren(invisibleRoot);
			}

			return getChildren(parent);
		}

		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject) child).getParent();
			}

			return null;
		}

		public Object[] getChildren(Object parent) {

			if (parent instanceof TreeParent) {
				return ((TreeParent) parent).getChildren();
			}

			return new Object[0];
		}

		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent) parent).hasChildren();
			return false;
		}

	}

	class ViewLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}

		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;


			if (obj instanceof TreeParent)
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}

	}

	public void initialize(String projectName) {
		TreeParent root = new TreeParent("WorkSpace Property Files");
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();

			IProject[] projects = workspace.getRoot().getProjects();
			
			for (int i = 0; i < projects.length; i++) {
				if (projectName != null && !projects[i].getName().equals(projectName)) continue;
				IProject project = projects[i];
				project.open(null /* IProgressMonitor */);
				IJavaProject javaProject = JavaCore.create(project);
				javaProjects.put(project.getName(), javaProject);
				
				String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
				List<URL> urlList = new ArrayList<URL>();
				for (int k = 0; k < classPathEntries.length; k++) {
					String entry = classPathEntries[k];
					IPath path = new Path(entry);
					URL url = path.toFile().toURI().toURL();
					urlList.add(url);
				}
				ClassLoader parentClassLoader = project.getClass().getClassLoader();
				URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
				URLClassLoader classLoader = new URLClassLoader(urls, parentClassLoader);
				
				Reflections reflections = new Reflections(classLoader, new MethodAnnotationsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
		        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Module.class);
		        
		        for (Class<?> clazz: annotated)
		        {
		        	TreeObject obj = new TreeObject(clazz
							.getName());
					root.addChild(obj);
		        }
				
//				IResource[] folderResources = projects[i].members();
//
//				for (int j = 0; j < folderResources.length; j++) {
//
//					if (folderResources[j] instanceof IFolder) {
//						IFolder resource = (IFolder) folderResources[j];
//						if (resource.getName().equalsIgnoreCase("Property Files")) {
//							IResource[] fileResources = resource.members();
//							for (int k = 0; k < fileResources.length; k++) {
//								if (fileResources[k] instanceof IFile &&           
//										fileResources[k].getName().endsWith(".properties")){
//									TreeObject obj = new TreeObject(fileResources[k]
//											.getName());
//									obj.setResouce(fileResources[k]);
//									root.addChild(obj);
//								}
//							}
//						}
//					}
//				}
			}
		}catch (Exception e) {
			// log exception
		}
		invisibleRoot = new TreeParent("");
		invisibleRoot.addChild(root);
	}

	public PropertyManagerView() {
	}

	public void createPartControl(Composite parent) {
		
		javaProjects = new HashMap<>();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		for (IProject project : projects) {
			try {
				project.open(null /* IProgressMonitor */);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			IJavaProject javaProject = JavaCore.create(project);
			javaProjects.put(project.getName(), javaProject);
		}
		
		
		new Label(parent, SWT.NULL).setText("Project:");
		Combo projectCombobox = new Combo(parent, SWT.NULL);
		//dogBreed.setItems(new String [] {"Collie", "Pitbull", "Poodle", "Scottie"});
		for (IProject project : projects)
			projectCombobox.add(project.getName());
		projectCombobox.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		projectCombobox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				String projectName = ((Combo)e.getSource()).getText();
				IJavaProject project = javaProjects.get(projectName);
				initialize(projectName);
				viewer.refresh();
				viewer.expandAll();
			}
			
		});
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(getViewSite());
		hookContextMenu();
		hookDoubleCLickAction();
	       
	}

	private void hookDoubleCLickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (!(obj instanceof TreeObject)) {
					return;
				}else {
					TreeObject tempObj = (TreeObject) obj;
					IFile ifile = ResourcesPlugin.getWorkspace().getRoot().
							getFile(tempObj.getResouce().getFullPath());
					IWorkbenchPage dpage =
							PropertyManagerView.this.getViewSite()
							.getWorkbenchWindow().getActivePage();
					if (dpage != null) {
						try {
							IDE.openEditor(dpage, ifile,true);
						}catch (Exception e) {
							// log exception
						}
					}
				}
			};
		});
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		Action refresh =new Action() {
			public void run() {
				initialize(null);
				viewer.refresh();
			}
		};
		refresh.setText("Refresh");
		menuMgr.add(refresh);
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
}