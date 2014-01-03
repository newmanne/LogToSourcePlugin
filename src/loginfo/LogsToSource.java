package loginfo;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Sets;

public class LogsToSource {
	
	private static boolean shouldScan(IProject project) {
		if (project.getName().startsWith("hadoop")) {
			return true;
		}
		return false;
	}
	
	public static void logToSource() throws IOException, CoreException {
		System.out.println("BEGIN RUN");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for (IProject project : projects) {
			// this one project seems to cause trouble
			if (shouldScan(project)) {
				System.out.println("Scanning project " + project.getName());
				try {
					IPackageFragment[] packageFragments = JavaCore.create(
							project).getPackageFragments();
					for (IPackageFragment packageFragment : packageFragments) {
						if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
							createAST(packageFragment);
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			};

		}

		System.out.println("END RUN");
	}

	private static void createAST(IPackageFragment packageFragment)
			throws JavaModelException {
		for (ICompilationUnit compilationUnit : packageFragment
				.getCompilationUnits()) {
			CompilationUnit ast = parse(compilationUnit);
			ast.accept(new MethodVisitor(ast));
		}
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

}
