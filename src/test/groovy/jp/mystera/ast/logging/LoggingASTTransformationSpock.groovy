package jp.mystera.ast.logging

import org.gmock.WithGMock
import spock.lang.Specification
import static org.hamcrest.Matchers.instanceOf
import spock.lang.Ignore

@WithGMock
class LoggingASTTransformationSpock extends Specification {
    def "メソッド呼び出しの開始と終了時にログを出力する"() {
        setup:
        def testClass = new TestClass()
        def mockLogger = mock()
        mockLogger.info(instanceOf(GString)).returns(true).times(2)
        testClass.log = mockLogger

        when:
        play {
            testClass.testMethod('aaaa', 'bbbb')
        }

        then:
        notThrown(Throwable)
    }

    def "＠WithLoggingアノテーションが付与されたクラスのインスタンスは、logフィールドを持つ"() {
        setup:
        def testClass = new TestClass()

        expect:
        testClass.metaClass.hasProperty(testClass, 'log')
    }

    @Ignore
    def "既にlogフィールドが存在するクラスのインスタンスには、logフィールドをさらに作成しない"() {
        setup:
        def testClass = new TestClassAlreadyHasLogField()
        
        expect:
        testClass.metaClass.hasProperty(testClass, 'log')
        testClass.log.class == String.class
    }
}

@WithLogging
class TestClass {
    def testMethod(args0, args1) {
        println "print method"
    }
}

class TestClassAlreadyHasLogField {
    def log = 'anotherLogger'
}