package loginfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;

import com.google.common.collect.ImmutableSet;

public class LogsToSource {

	public static void logToSource() throws IOException, CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for (IProject project : projects) {
			if (!(project.getName().equals("RemoteSystemsTempFiles"))) {
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

		// IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// System.out.println(root);
		// IProject project = root.getProject("HelloWorld");
		// project.open(null);
		//
		// IJavaProject javaProject = JavaCore.create(project);
		// IType lwType = javaProject.findType("HelloWorld");
		// ICompilationUnit lwCompilationUnit = lwType.getCompilationUnit();

		System.out.println("END RUN");
	}

	private static void createAST(IPackageFragment packageFragment)
			throws JavaModelException {
		for (ICompilationUnit compilationUnit : packageFragment
				.getCompilationUnits()) {
			CompilationUnit ast = parse(compilationUnit);
			ast.accept(new LogToSourceVisitor(ast));
		}
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	public static class LogToSourceVisitor extends ASTVisitor {

		private CompilationUnit compilationUnit;

		public LogToSourceVisitor(CompilationUnit compilationUnit) {
			this.compilationUnit = compilationUnit;
		}

		Set<String> loggingFunctions = ImmutableSet.of("println");

		public String parseRegex(ASTNode node) {
			String regex = "";
			if (node instanceof StringLiteral) {
				regex += ((StringLiteral) node).getLiteralValue();
			} else if (node instanceof InfixExpression) {
				Operator operator = ((InfixExpression) node).getOperator();
				assert operator.toString().equals("+");
				regex += parseRegex(((InfixExpression) node).getLeftOperand());
				regex += parseRegex(((InfixExpression) node).getRightOperand());
				List<ASTNode> extendedOperands = ((InfixExpression) node)
						.extendedOperands();
				for (ASTNode op : extendedOperands) {
					regex += parseRegex(op);
				}
			} else if (node instanceof SimpleName) {
				// TODO: get this working, right now its null because of
				// http://stackoverflow.com/questions/2017945/bindings-not-resolving-with-ast-processing-in-eclipse/5803778#5803778
				IBinding resolveBinding = ((SimpleName) node).resolveBinding();
				String identifier = ((SimpleName) node).getIdentifier();
				regex += "[variable:" + identifier + "]";
			}

			return regex;
		}

		private boolean isLoggingFunction(String name) {
			return loggingFunctions.contains(name.toString());
		}

		public boolean visit(MethodInvocation node) {
			if (isLoggingFunction(node.getName().toString())) {
				String regex = "";
				List<ASTNode> arguments = node.arguments();
				for (ASTNode arg : arguments) {
					regex += parseRegex(arg);
				}
				System.out.println("Found log statement:" + regex);
				int lineNumber = compilationUnit.getLineNumber(node
						.getStartPosition());
				System.out.println("At line number:" + lineNumber);
			}
			return false;
		}

		// TODO: map to source
		// TODO: watch for toString() functions
		// TODO: watch for the variable to be declared, so we know its type?
		// TODO: should regex be a class, where we slowly evaluate the parts
		// we don' know?

		// TODO: look at this for assigning final string variables/
		//
		// public boolean visit(VariableDeclarationFragment node) {
		// SimpleName name = node.getName();
		// this.names.add(name.getIdentifier());
		// System.out.println("Declaration of '"+name+"' at line"+compilationUnit.getLineNumber(name.getStartPosition()));
		// return false; // do not continue to avoid usage info
		// }
		//
		// TODO: maybe useful?
		// public boolean visit(StringLiteral node) {
		// node.getLiteralValue();
		// System.out.println(node.getLiteralValue());
		// return false;
		// }
	}

}
