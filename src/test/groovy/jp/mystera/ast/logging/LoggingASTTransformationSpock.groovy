package jp.mystera.ast.logging

import org.gmock.WithGMock
import spock.lang.Ignore
import spock.lang.Specification

@WithGMock
class LoggingASTTransformationSpock extends Specification {
    def "メソッド呼び出しの開始と終了時にログを出力する"() {
        setup:
        def testClass = new TestClass()
        def mockLogger = mock()
        mockLogger.info('Start testMethod(aaaa, bbbb)').returns(true).once()
        mockLogger.info('End testMethod(aaaa, bbbb)').returns(true).once()
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