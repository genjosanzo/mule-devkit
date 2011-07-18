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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.param.InboundHeaders;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Module(name = "headers")
public class HeadersModule {
    @Processor
    public String processHeader(@InboundHeaders("foo") String foo) {
        return foo;
    }

    @Processor
    public String processHeaderOptional(@InboundHeaders("faz?") String faz) {
        if (faz == null) {
            return "faz not set";
        }
        return faz;
    }

    @Processor
    public URL processHeaderWithType(@InboundHeaders("url") URL apple) {
        return apple;
    }

    @Processor
    public Map<?, ?> processHeaders(@InboundHeaders("foo, bar") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public Map<?, ?> processHeadersAll(@InboundHeaders("*") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public Map<?, ?> processHeadersWildcard(@InboundHeaders("MULE_*") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public Map<?, ?> processHeadersMultiWildcard(@InboundHeaders("MULE_*, ba*") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public Map<?, ?> processSingleMapHeader(@InboundHeaders("foo") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public Map<?, ?> processHeadersOptional(@InboundHeaders("foo, bar, baz?") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public Map<?, ?> processHeadersAllOptional(@InboundHeaders("foo?, bar?") Map<?, ?> headers) {
        return headers;
    }

    @Processor
    public List<?> processHeadersList(@InboundHeaders("foo, bar, baz") List<?> headers) {
        return headers;
    }

    @Processor
    public List<?> processHeadersListAll(@InboundHeaders("*") List<?> headers) {
        return headers;
    }

    @Processor
    public List<?> processSingleHeaderList(@InboundHeaders("foo") List<?> headers) {
        return headers;
    }

    @Processor
    public List<?> processHeadersListOptional(@InboundHeaders("foo, bar, baz?") List<?> headers) {
        return headers;
    }

    @Processor
    public List<?> processHeadersListAllOptional(@InboundHeaders("foo?, bar?") List<?> headers) {
        return headers;
    }

    @Processor
    public List<?> processHeadersListWildcard(@InboundHeaders("MULE_*") List<?> headers) {
        return headers;
    }

    @Processor
    public List<?> processHeadersListMultiWildcard(@InboundHeaders("MULE_*, ba*") List<?> headers) {
        return headers;
    }

    @Transformer
    public URL transformStringToUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
