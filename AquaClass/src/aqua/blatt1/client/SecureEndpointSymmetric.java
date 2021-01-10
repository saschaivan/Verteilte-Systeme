package aqua.blatt1.client;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.InetSocketAddress;

public class SecureEndpointSymmetric extends Endpoint {

    private SecretKeySpec symmetricKey;
    private Cipher encrypt;
    private Cipher decrypt;

    public SecureEndpointSymmetric(int port) {
        super(port);
        setup();
    }

    public SecureEndpointSymmetric() {
        super();
        setup();
    }

    public void setup() {
        this.symmetricKey = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
        try {
            encrypt = Cipher.getInstance("AES");
            decrypt = Cipher.getInstance("AES");
            encrypt.init(Cipher.ENCRYPT_MODE, symmetricKey);
            decrypt.init(Cipher.DECRYPT_MODE, symmetricKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        SealedObject sealed;
        try {
            sealed = new SealedObject(payload, encrypt);
            super.send(receiver, sealed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message nonBlockingReceive() {
        return super.nonBlockingReceive();
    }

    @Override
    public Message blockingReceive() {
        Message encodedMessage = super.blockingReceive();
        SealedObject sealedpayload = (SealedObject) encodedMessage.getPayload();
        Serializable payload = null;
        try {
            payload = (Serializable) sealedpayload.getObject(decrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Message(payload, encodedMessage.getSender());
    }
}
