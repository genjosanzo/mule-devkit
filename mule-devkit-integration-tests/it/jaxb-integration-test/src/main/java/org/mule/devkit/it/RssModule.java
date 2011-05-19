package org.mule.devkit.it;

import org.mule.devkit.it.rss.RssChannel;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Processor;

@Module(name = "rss")
public class RssModule
{
    @Processor
    public int itemCount(RssChannel channel)
    {
        return channel.getItem().size();
    }
}
