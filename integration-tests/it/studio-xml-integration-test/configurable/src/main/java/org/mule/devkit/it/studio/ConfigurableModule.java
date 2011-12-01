package org.mule.devkit.it.studio;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

import java.net.URL;

/**
 * Connector class
 *
 * @author MuleSoft inc
 */
@Module(name = "configurable")
public class ConfigurableModule {

    /**
     * Configurable String
     */
    @Configurable
    private String configurableString;
    /**
     * Configurable optional String
     */
    @Configurable
    @Optional
    private String optionalConfigurableString;
    /**
     * Configurable optional String with default value
     */
    @Configurable
    @Optional
    @Default("a default")
    private String optionalWithDefaultConfigurableString;

    /**
     * Configurable URL
     */
    @Configurable
    @Optional
    @Default("http://myUrl:9999")
    private URL url;

    /**
     * Configurable enumerated
     */
    @Configurable
    @Optional
    @Default("NO")
    private SiNoEnum siNoEnum;

    public enum SiNoEnum {
        SI, NO
    }
}