package jp.mystera.ast.logging

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.gcontracts.annotations.Requires
import org.codehaus.groovy.ast.expr.*

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class LoggingASTTransformation implements ASTTransformation {

    private static final String LOG_FIELD_NAME = 'log'
    private static final String LOGGING_LEVEL_INFO = "info"

    @Requires({
        astNodes && astNodes[0] && astNodes[0] instanceof AnnotationNode \
        && astNodes[0].classNode?.name == WithLogging.class.name \
        && astNodes[1]
    })
    void visit(org.codehaus.groovy.ast.ASTNode[] astNodes, org.codehaus.groovy.control.SourceUnit sourceUnit) {
        List<ClassNode> classes = sourceUnit.AST?.classes
        classes.findAll {it.getAnnotations(new ClassNode(jp.mystera.ast.logging.WithLogging))}.each { ClassNode currentClassNode ->
            currentClassNode.addField(
                    LOG_FIELD_NAME,
                    FieldNode.ACC_PRIVATE,
                    new ClassNode(Object),
                    new MethodCallExpression(
                            new ClassExpression(new ClassNode(LogFactory)),
                            new ConstantExpression("get${LOG_FIELD_NAME.capitalize()}"),
                            new ArgumentListExpression(new ConstantExpression(currentClassNode.name))
                    )
            )

            currentClassNode.methods.each { MethodNode method ->
                BlockStatement blockStatement = method.code
                assert blockStatement
                List<Statement> existingStatement = blockStatement.statements
                existingStatement.add(0, createStartLogStatement(method))
                existingStatement.add(createEndLogStatement(method))
            }
        }
    }

    private Statement createStartLogStatement(MethodNode method) {
        return createLogStatement('Start ', method)
    }

    private Statement createEndLogStatement(MethodNode method) {
        return createLogStatement('End ', method)
    }

    private Statement createLogStatement(String prefix, MethodNode method) {
        String verbatimText = createVerbatimText(prefix, method)

        List<VariableExpression> variableExpressionList = createVariableExpressionList(method)

        List<ConstantExpression> constantExpressionList = createConstantExpressionList(prefix, method)

        def result = new ExpressionStatement(
                new MethodCallExpression(
                        new VariableExpression(LOG_FIELD_NAME),
                        new ConstantExpression(LOGGING_LEVEL_INFO),
                        new ArgumentListExpression(
                                new GStringExpression(verbatimText, constantExpressionList, variableExpressionList)
                        )
                )
        )
        return result
    }

    private createConstantExpressionList(String prefix, MethodNode method) {
        List<ConstantExpression> constantExpressionList = [new ConstantExpression(prefix + method.name + '(')]
        (1..method.parameters.size() - 1).each {constantExpressionList << new ConstantExpression(', ')}
        constantExpressionList << new ConstantExpression(')')
        return constantExpressionList
    }

    private createVariableExpressionList(MethodNode method) {
        List<VariableExpression> variableExpressionList = method.parameters.collect {
            new VariableExpression(it.name)
        }
        return variableExpressionList
    }

    private createVerbatimText(String prefix, MethodNode method) {
        String verbatimText = prefix + method.name + '('
        method.parameters.each {
            verbatimText += '${' + it.name + '}, '
        }
        verbatimText = verbatimText.substring(0, verbatimText.size() - 2)
        verbatimText += ')'
        return verbatimText
    }
}
