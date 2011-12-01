package org.mule.devkit.it.studio;

import org.mule.api.ConnectionException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.InvalidateConnectionOn;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

import java.net.URL;

/**
 * Connector class
 *
 * @author MuleSoft inc
 */
@Connector(name = "connector")
public class ConnectorModule {

    /**
     * a URL
     */
    @Configurable
    @Optional
    @Default("http://www.mulesoft.org")
    private URL url;
    private String username;
    private String password;

    /**
     * Processor method that invalidates connections
     */
    @Processor
    @InvalidateConnectionOn(exception = RuntimeException.class)
    public void invalidate() {
    }

    /**
     * returns the username
     *
     * @return the username
     */
    @Processor
    public String getUsername() {
        return username;
    }

    /**
     * Connect method
     *
     * @param username the username to use
     * @param password the password to use
     * @throws ConnectionException
     */
    @Connect
    public void connect(@ConnectionKey String username, String password) throws ConnectionException {
    }

    /**
     * Disconnect method
     */
    @Disconnect
    public void disconnect() {
    }

    /**
     * Connection identifier method
     *
     * @return the connection identifier
     */
    @ConnectionIdentifier
    public String connectionId() {
        return "";
    }

    /**
     * Is connected method
     *
     * @return whether it is connected
     */
    @ValidateConnection
    public boolean isConnected() {
        return true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}