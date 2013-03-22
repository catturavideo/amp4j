
package org.amp4j.internet;

public interface IProtocol {
    public interface IFactory {
        IProtocol buildProtocol(Object addr);
    }

    void makeConnection(ITransport transport);
    void dataReceived(byte[] data);
    void connectionLost(Throwable reason);
}
