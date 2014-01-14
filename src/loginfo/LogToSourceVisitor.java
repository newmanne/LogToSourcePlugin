package loginfo;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class LogToSourceVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	private final List<Integer> lineNumbers = Lists.newArrayList();
	final Set<String> loggingFunctions = ImmutableSet.of("info", "debug", "warn", "error");

	public LogToSourceVisitor(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	private boolean isLoggingFunction(MethodInvocation node) {
		return loggingFunctions.contains(node.getName().toString());
	}

	public boolean visit(MethodInvocation node) {
		if (isLoggingFunction(node)) {
			final int lineNumber = compilationUnit.getLineNumber(node
					.getStartPosition());
			lineNumbers.add(lineNumber);
			String regex = "";
			List<ASTNode> arguments = node.arguments();
			for (ASTNode arg : arguments) {
				regex += parseRegex(arg);
			}
			System.out.println(regex);
		}
		return false;
	}

	/** return true if at least one log statement was found in the method */
	public boolean foundMatch() {
		return !lineNumbers.isEmpty();
	}

	public List<Integer> getLineNumbers() {
		return lineNumbers;
	}
	
	public String parseRegex(ASTNode node) {
		return parseRegex(new StringBuilder(), node);
	}

	public String parseRegex(StringBuilder regex, ASTNode node) {
		if (node instanceof StringLiteral) {
			regex.append(((StringLiteral) node).getLiteralValue());
		} else if (node instanceof InfixExpression) {
			Operator operator = ((InfixExpression) node).getOperator();
			assert operator.toString().equals("+");
			regex.append(parseRegex(((InfixExpression) node).getLeftOperand()));
			regex.append(parseRegex(((InfixExpression) node).getRightOperand()));
			List<ASTNode> extendedOperands = ((InfixExpression) node)
					.extendedOperands();
			for (ASTNode op : extendedOperands) {
				regex.append(parseRegex(op));
			}
		} else if (node instanceof SimpleName) {
			String identifier = ((SimpleName) node).getIdentifier();
			ITypeBinding typeBinding = ((SimpleName) node).resolveTypeBinding();
			regex.append("[identifier: " + identifier + " type: " + typeBinding.getName() + "]");
		} else if (node instanceof NumberLiteral){
			regex.append(((NumberLiteral) node).getToken());
		} else if (node instanceof MethodInvocation) {
			ITypeBinding typeBinding = ((MethodInvocation) node).resolveTypeBinding();
			regex.append("[methodcall return value of type: " + typeBinding.getName() + "]");
		} else {
			System.out.println(node.getClass());
		}
		return regex.toString();
	}
}