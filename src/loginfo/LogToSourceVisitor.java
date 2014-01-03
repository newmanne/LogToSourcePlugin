package loginfo;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class LogToSourceVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	private boolean foundMatch = false;
	private final List<Integer> lineNumbers = Lists.newArrayList();

	public LogToSourceVisitor(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	Set<String> loggingFunctions = ImmutableSet.of("info");

	private boolean isLoggingFunction(MethodInvocation node) {
		return loggingFunctions.contains(node.getName().toString());
	}

	public boolean visit(MethodInvocation node) {
		if (isLoggingFunction(node)) {
			foundMatch = true;
			int lineNumber = compilationUnit.getLineNumber(node
					.getStartPosition());
			lineNumbers.add(lineNumber);
		}
		return false;
	}
	
	/** return true if at least one log statement was found in the method */
	public boolean foundMatch() {
		return foundMatch;
	}

	public List<Integer> getLineNumbers() {
		return lineNumbers;
	}
}