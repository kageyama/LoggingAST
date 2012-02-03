package jp.mystera.ast.logging

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.expr.*
import org.gcontracts.annotations.Requires
import org.codehaus.groovy.ast.AnnotationNode

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class LoggingASTTransformation implements ASTTransformation {

    @Requires({
        astNodes && astNodes[0] && astNodes[0] instanceof AnnotationNode \
        && astNodes[0].classNode?.name == WithLogging.class.name \
        && astNodes[1]
    })
    void visit(org.codehaus.groovy.ast.ASTNode[] astNodes, org.codehaus.groovy.control.SourceUnit sourceUnit) {
        List<ClassNode> classes = sourceUnit.AST?.classes
        classes.findAll {it.getAnnotations(new ClassNode(jp.mystera.ast.logging.WithLogging))}.each { ClassNode currentClassNode ->
            currentClassNode.addField(
                    'log',
                    FieldNode.ACC_PRIVATE,
                    new ClassNode(Object),
                    new MethodCallExpression(
                            new ClassExpression(new ClassNode(LogFactory)),
                            new ConstantExpression("getLog"),
                            new ArgumentListExpression(new ConstantExpression(currentClassNode.name))
                    )
            )

            currentClassNode.methods.each { MethodNode method ->
                BlockStatement blockStatement = method.code
                assert blockStatement
                List<Statement> existingStatement = blockStatement.statements
                existingStatement.add(0, createStartLogStatement(method))
                existingStatement.add(createStartLogStatement(method))
            }
        }
    }

    private Statement createStartLogStatement(MethodNode method) {
        String methodName = method.name
        Parameter[] parameters = method.parameters

        String verbatimText = methodName + '('
        parameters.each {
            verbatimText += '${' + it.name + '}, '
        }
        verbatimText = verbatimText.substring(0, verbatimText.size() - 2)
        verbatimText += ')'

        List<VariableExpression> variableExpressionList = parameters.collect {
            new VariableExpression(it.name)
        }

        List<ConstantExpression> constantExpressionList = [new ConstantExpression(methodName + '(')]
        (1..parameters.size() - 1).each {constantExpressionList << new ConstantExpression(', ')}
        constantExpressionList << new ConstantExpression(')')

        def result = new ExpressionStatement(
                new MethodCallExpression(
                        new VariableExpression("log"),
                        new ConstantExpression("info"),
                        new ArgumentListExpression(
                                new GStringExpression(verbatimText, constantExpressionList, variableExpressionList)
                        )
                )
        )
        return result
    }
}
