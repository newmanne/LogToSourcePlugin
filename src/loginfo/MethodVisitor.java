package loginfo;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodVisitor extends ASTVisitor {
	
	private CompilationUnit compilationUnit;
	
	public MethodVisitor(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}
	
	public boolean visit(MethodDeclaration node) {
		final String methodName = node.getName().getFullyQualifiedName();
		final String className = node.getParent() instanceof TypeDeclaration ? ((TypeDeclaration) node.getParent()).getName().toString() : "anonymous class";

		// look through the method for logging statements
		LogToSourceVisitor logToSourceVisitor = new LogToSourceVisitor(compilationUnit);
		node.accept(logToSourceVisitor);
		
		// if there is a log statement found, spit out the callers of the function with the log statement
		if (logToSourceVisitor.foundMatch()) {
			System.out.println("Log statements were found in class " + className +  " in method " + methodName + " at line numbers " + logToSourceVisitor.getLineNumbers() + "\n Called from: ");
			try {
				Callers.execute(node.resolveBinding().getJavaElement());
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}