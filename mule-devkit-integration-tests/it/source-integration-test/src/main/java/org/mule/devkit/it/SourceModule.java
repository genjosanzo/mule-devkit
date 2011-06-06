package org.mule.devkit.it;

import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.annotations.SourceCallback;

@Module(name = "source")
public class SourceModule
{
    @Source
    public void count(int startAt, int endAt, int step, SourceCallback callback) throws InterruptedException
    {
		int count = startAt;
        while(true)
        {
            if(Thread.interrupted() || count == endAt)
                throw new InterruptedException();

            callback.process(count);

			count += step;
        }	
    }

}
