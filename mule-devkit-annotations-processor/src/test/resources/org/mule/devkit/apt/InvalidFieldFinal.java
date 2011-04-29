package org.mule.devkit.apt;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;

@Module(name="final")
public class InvalidFieldFinal {
    @Configurable
    private final String finalField = "";
}
