package org.mule.devkit.it.studio;

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.InvocationHeaders;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.api.annotations.param.Payload;

import java.util.Map;

/**
 * Connector class
 *
 * @author MuleSoft inc
 */
@Module(name = "parameter-annotations")
@OAuth(requestTokenUrl = "", accessTokenUrl = "", authorizationUrl = "")
public class ParameterAnnotationsModule {

    @OAuthConsumerKey
    private String consumerKey;
    @OAuthConsumerSecret
    private String consumerSecret;

    /**
     * Method that takes optional parameters
     *
     * @param aString    a optional string parameter
     * @param aInteger   a optional integer parameter
     * @param aBoolean   a optional boolean parameter
     * @param aLong      a optional long parameter
     * @param aFloat     a optonal float parameter
     * @param aDouble    a optional double parameter
     * @param aCharacter a optional character parameter
     */
    @Processor
    public void optionalParams(@Optional String aString,
                               @Optional Integer aInteger,
                               @Optional Boolean aBoolean,
                               @Optional Long aLong,
                               @Optional Float aFloat,
                               @Optional Double aDouble,
                               @Optional Character aCharacter) {
    }

    /**
     * Method that takes optional parameters with default values
     *
     * @param aString    a optional string parameter with default value
     * @param aInteger   a optional integer parameter with default value
     * @param aBoolean   a optional boolean parameter with default value
     * @param aLong      a optional long parameter with default value
     * @param aFloat     a optonal float parameter with default value
     * @param aDouble    a optional double parameter with default value
     * @param aCharacter a optional character parameter with default value
     */
    @Processor
    public void optionalParamsWithDefaults(@Optional @Default("fede") String aString,
                                           @Optional @Default("1") int aInteger,
                                           @Optional @Default("false") boolean aBoolean,
                                           @Optional @Default("2") long aLong,
                                           @Optional @Default("3.2") float aFloat,
                                           @Optional @Default("5.3") double aDouble,
                                           @Optional @Default("A") char aCharacter) {
    }

    /**
     * Method that takes the payload
     *
     * @param payload the payload
     */
    @Processor
    public void payload(@Payload Object payload) {
    }

    /**
     * Method that takes the inbound headers
     *
     * @param inboundHeaders the inbound headers
     */
    @Processor
    public void inboundHeaders(@InboundHeaders("*") Map<String, Object> inboundHeaders) {

    }

    /**
     * Method that takes the invocation headers
     *
     * @param invocationHeaders the invocation headers
     */
    @Processor
    public void invocationHeaders(@InvocationHeaders("*") Map<String, Object> invocationHeaders) {
    }

    /**
     * Method that takes the outbound headers
     *
     * @param outboundHeaders the outbound headers
     */
    @Processor
    public void outboundHeaders(@OutboundHeaders Map<String, Object> outboundHeaders) {
    }

    /**
     * Method that takes the access token and token secret
     *
     * @param token  the oauth access token
     * @param secret the oauth access token secret
     */
    @Processor
    public void pauth(@OAuthAccessToken String token, @OAuthAccessTokenSecret String secret) {
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }
}