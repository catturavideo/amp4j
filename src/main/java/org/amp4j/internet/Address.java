package org.amp4j.internet;

public class Address implements IAddress {
    private String host;
    private int port;
    
    Address(String host, int port)
    {
        this.host = host;
        this.port = port;
    }
    
    public String getHost()
    {
        return host;
    }
    
    public int getPort()
    {
        return port;
    }
}
