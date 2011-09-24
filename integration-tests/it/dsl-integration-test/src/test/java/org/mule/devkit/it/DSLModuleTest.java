/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.it;

import org.junit.Test;
import org.mule.devkit.it.dsl.DSLModule;
import org.mule.config.dsl.*;

import static org.mule.config.dsl.expression.CoreExpr.payload;
import static org.junit.Assert.*;

public class DSLModuleTest {

    @Test
    public void testMule() throws Exception {

        final Simple mySimple = new Simple();

        final Mule mule = Mule.newInstance(new AbstractModule() {
            @Override
            public void configure() {
                final DSLModule module = new DSLModule();
                module.setConfigValue("myGeneralConfig");

                flow("MyTestFlow")
                        .process(module.processSometing(payload()))
                        .process(module.checkProcessValue("something"))
                        .process(module.checkConfigValue("myGeneralConfig"))
                        .invoke(mySimple).methodName("invoke").withoutArgs();
            }
        });

        mule.start();

        Thread.sleep(4000L);

        mule.flow("MyTestFlow").process("something");

        Thread.sleep(500L);

        assertEquals(true, mySimple.isExecuted());

        mule.stop();
    }


    public static class Simple {
        boolean executed = false;

		public void invoke() {
            executed = true;
        }

        public boolean isExecuted() {
            return executed;
        }

    }


}