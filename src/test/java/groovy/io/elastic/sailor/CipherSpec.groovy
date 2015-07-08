package groovy.io.elastic.sailor
import com.google.gson.JsonObject
import io.elastic.sailor.CipherWrapper
import spock.lang.Specification
import io.elastic.api.Message

class CipherSpec extends Specification {

    def key = "testCryptoPassword";
    def iv ="iv=any16_symbols".getBytes();

    def "should encrypt & decrypt strings"() {
        given:
            def content = "Hello world"
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.encrypt(content)
            def decryptedResult = cipher.decrypt(result)
        then:
            decryptedResult.toString() == content.toString()
    }

    def "should encrypt & decrypt objects"() {
        given:
            def content = new JsonObject()
            content.addProperty("property1", "Hello world")
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.encryptMessageContent(content)
            def decryptedResult = cipher.decryptMessageContent(result)
        then:
            decryptedResult.toString() == content.toString()
    }

    def "should throw error if failed to decrypt"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            cipher.decrypt("dsdasdsad");
        then: // TODO: should throw RuntimeException if input string is not JsonElement
            thrown(RuntimeException)
    }

    def "should decrypt JSON objects encrypted in Node.js"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.decryptMessageContent("MhcbHNshDRy6RNubmFJ+u4tcKKTKT6H50uYMyBXhws1xjvVKRtEC0hEg0/R2Zecy");
        then:
            result.get("someKey").getAsString() == "someValue"
    }

    def "should encrypt JSON objects so that Node.js understands"() {
        given:
            def cipher = new CipherWrapper(key, iv)
            def body = new JsonObject()
            body.addProperty("someKey", "someValue")
        when:
            def result = cipher.encryptMessageContent(body);
        then:
            result == "MhcbHNshDRy6RNubmFJ+u4tcKKTKT6H50uYMyBXhws1xjvVKRtEC0hEg0/R2Zecy"
    }

    def getMessage(){
        def body = new JsonObject()
        body.addProperty("incomingProperty1", "incomingValue1")
        body.addProperty("incomingProperty2", "incomingValue2")

        def attachments = new JsonObject()
        attachments.addProperty("incomingAttachment1", "incomingAttachment1Content")
        attachments.addProperty("incomingAttachment2", "incomingAttachment2Content")

        return new Message(body, attachments);
    }

    def "should encrypt message"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.encryptMessage(getMessage());
        then:
            result == "TOxRVfC2S4QDUzw6tzpoVNzi5ldNj+qGGrx2bMJLTn+0mgv3+xZNxMPHI5HdsTq+pBF3oXzgNmaFkXWGou0rPkyhSdpk/" +
                    "ZjI6YciJrFhtOk9Bgh5ScAO/cZYChDertRLGjGNtm4/XTVdYCw5LBdyYDSoGfYt2K+09NtzoOGrK4KGAKhZm4BaEfCFTeGU" +
                    "vXpSCaiUxaHxro7OpxvO1Y5EA/ZBJIXWjhTMyc8E0WF12+wCq1eByfl5WXvEOqksfk1FGOIjqxCn9UEo995Y2f0YMA=="
    }

    def "IV-powered cipher should encrypt & decrypt objects"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            def encrypt = cipher.encrypt(getMessage().toString());
            def decrypt = cipher.decrypt(encrypt);
        then:
            getMessage().toString() == decrypt
    }
}
