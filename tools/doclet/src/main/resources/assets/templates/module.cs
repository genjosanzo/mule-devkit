<?cs set:section = "mule" ?>
<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="<?cs var:class.since.key ?>">
<?cs call:custom_masthead() ?>

<div class="g-unit" id="all-content">

<div id="api-info-block">

<div class="sum-details-links">
<?cs if:doclava.generate.sources ?>
<div>
<a href="<?cs var:class.moduleName ?>-schema.html">View Schema</a>
</div>
<?cs /if ?>
Summary:
<a href="#lconfig">Configuration</a>
<?cs if:subcount(class.methods.processor) ?>
  &#124; <a href="#promethods">Message Processors</a>
<?cs /if ?>
<?cs if:subcount(class.methods.source) ?>
  &#124; <a href="#soumethods">Message Sources</a>
<?cs /if ?>
<?cs if:subcount(class.methods.transformer) ?>
  &#124; <a href="#trnasmethods">Transformers</a>
<?cs /if ?>
</div><!-- end sum-details-links -->

<div class="api-level">
  <?cs call:since_tags(class) ?>
  <?cs call:federated_refs(class) ?>
</div>
</div><!-- end api-info-block -->

<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ======== START OF MODULE DATA ======== -->

<div id="jd-header">
    <?cs var:project.groupId ?>
<h1><?cs var:project.artifactId ?></h1>

</div><!-- end header -->

<div id="naMessage"></div>

<div id="jd-content" class="api apilevel-<?cs var:class.since.key ?>">
<?cs # this next line must be exactly like this to be parsed by eclipse ?>

<div class="jd-tagdata">
      <table class="jd-tagtable">
        <tbody><tr>
          <th>Namespace</th><td><?cs var:class.moduleNamespace ?></td>
        </tr><tr>
          <th>Schema Location</th><td><?cs var:class.moduleSchemaLocation ?>&nbsp;&nbsp;(<a href="<?cs var:class.moduleSchemaPath ?>">View Schema</a>)</td>
        </tr>
<tr>
          <th>Schema Version</th><td><?cs var:class.moduleVersion ?></td>
        </tr>
<tr>
          <th>Minimum Mule Version</th><td><?cs var:class.moduleMinMuleVersion ?></td>
        </tr>
    </tbody></table>
  </div>

<div class="jd-descr">
<?cs call:deprecated_warning(class) ?>
<?cs if:subcount(class.descr) ?>
<h2>Module Overview</h2>
<p><?cs call:op_tag_list(class.descr) ?></p>
<?cs /if ?>

</div><!-- jd-descr -->


<?cs # summary macros ?>

<?cs def:write_op_summary(methods, included) ?>
<?cs set:count = #1 ?>
<?cs each:method = methods ?>
	 <?cs # The apilevel-N class MUST BE LAST in the sequence of class names ?>
    <tr class="<?cs if:count % #2 ?>alt-color<?cs /if ?> api apilevel-<?cs var:method.since.key ?>" >
        <td class="jd-linkcol" width="100%"><nobr>
        <span class="sympad"><?cs call:cond_link("&lt;" + class.moduleName + ":" + method.elementName + "&gt;", toroot + "mule/", method.modhref, included) ?></nobr>
        <?cs if:subcount(method.shortDescr) || subcount(method.deprecated) ?>
        <div class="jd-descrdiv"><?cs call:op_short_descr(method) ?></div>
        <?cs /if ?>
  </td></tr>
<?cs set:count = count + #1 ?>
<?cs /each ?>
<?cs /def ?>

<?cs def:write_config_summary(fields) ?>
<?cs /def ?>


<?cs # end macros ?>

<div class="jd-descr">
<?cs # make sure there's a summary view to display ?>
<?cs if:subcount(class.config)
     || subcount(class.methods.processor)
     || subcount(class.methods.source)
     || subcount(class.methods.transformer) ?>
<h2>Summary</h2>

<!-- Config -->
<?cs if:subcount(class.config) ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- =========== CONFIG SUMMARY =========== -->
<table id="lconfig" class="jd-sumtable"><tr><th colspan="12">Configuration</th></tr>
	 <?cs # The apilevel-N class MUST BE LAST in the sequence of class names ?>
    <tr class="<?cs if:count % #2 ?>alt-color<?cs /if ?> api apilevel-<?cs var:method.since.key ?>" >
        <td class="jd-linkcol" width="100%"><nobr>
        <span class="sympad"><?cs call:link("&lt;" + class.moduleName + ":config&gt;", toroot + "mule/", class.moduleName + ".html#config") ?></nobr>
        <div class="jd-descrdiv">Configure an instance of this module</div>
  </td></tr>
</table>
<?cs /if ?>

<?cs if:subcount(class.methods.processor) ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========== MESSAGE PROCESSOR SUMMARY =========== -->
<table id="promethods" class="jd-sumtable"><tr><th colspan="12">Message Processors</th></tr>
<?cs call:write_op_summary(class.methods.processor, 1) ?>
</table>
<?cs /if ?>

<?cs if:subcount(class.methods.source) ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========== MESSAGE SOURCE SUMMARY =========== -->
<table id="soumethods" class="jd-sumtable"><tr><th colspan="12">Message Sources</th></tr>
<?cs call:write_op_summary(class.methods.source, 1) ?>
</table>
<?cs /if ?>

<?cs if:subcount(class.methods.transformer) ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========== TRANSFORMER SUMMARY =========== -->
<table id="trnasmethods" class="jd-sumtable"><tr><th colspan="12">Transformers</th></tr>
<?cs call:write_op_summary(class.methods.transformer, 1) ?>
</table>
<?cs /if ?>

<?cs /if ?>

</div><!-- jd-descr (summary) -->

<?cs if:subcount(class.config) ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========= CONFIGURATION DETAIL ======== -->
<A NAME="config"></A>
<h2>Configuration</h2>
<p>To use the this module within a flow the namespace to the module must be included. The resulting flow will look
similar to the following:</p>
<pre>
&lt;mule xmlns="http://www.mulesoft.org/schema/mule/core"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:<?cs var:class.moduleName ?>="<?cs var:class.moduleNamespace ?>"
      xsi:schemaLocation="
               http://www.mulesoft.org/schema/mule/core
               http://www.mulesoft.org/schema/mule/core/current/mule.xsd
               <?cs var:class.moduleNamespace ?>
               <?cs var:class.moduleSchemaLocation ?>"&gt;

      &lt;!-- here goes your flows and configuration elements --&gt;

&lt;/mule&gt;
</pre>
<p>This module is configured using the <i>config</i> element. This element must be placed outside of your flows and at
the root of your Mule application. You can create as many configurations as you deem necesary as long as each carries
its own name.</p>
<p>Each message processor, message source or transformer carries a <i>config-ref</i> attribute that allows the invoker to
specify which configuration to use.</p>
<table id="lconfig" class="jd-sumtable">
<tr><th colspan="12">Attributes</th></tr>
<tr><th>Type</th><th>Name</th><th>Default Value</th><th>Description</th></tr>
<td class="jd-typecol"><nobr>xs:string</nobr></td>
<td class="jd-linkcol"><nobr>name</nobr></td>
<td class="jd-descrcol"></td>
<td class="jd-descrcol" width="100%"><i>Optional.&nbsp;</i>Give a name to this configuration so it can be later referenced.</td>
<?cs set:count = #2 ?>
    <?cs each:field=class.config ?>
      <tr class="<?cs if:count % #2 ?>alt-color<?cs /if ?> api apilevel-<?cs var:field.since.key ?>" >
          <td class="jd-typecol"><nobr>
          <?cs var:field.type.xmlName ?></nobr></td>
          <td class="jd-linkcol"><nobr><?cs var:field.name ?></nobr></td>
          <td class="jd-descrcol"><?cs var:field.defaultValue ?></td>
          <td class="jd-descrcol" width="100%"><?cs if:field.optional=="true" ?><i>Optional.&nbsp;</i><?cs /if ?><?cs call:short_descr(field) ?></td>
      </tr>
      <?cs set:count = count + #1 ?>
    <?cs /each ?>
</table>
<?cs /if ?>

<?cs if:class.moduleSessionAware=="true" ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========= CONNECTION MANAGEMENT DETAIL ======== -->
<h2>Connection Pool</h2>
<p>This connector offers automatic connection management via the use of a connection pool. The pool will act a storage mechanism for all the connections that are in-use by the user of
this connector.</p>
<p>Prior to execution of a processor, the connector will attempt to lookup an already established connection and if one doesn't exists it will
create one. That lookup mechanism is done in the connection pool via the use of connection variables declared as keys.</p>
<p>The user of the connector can configure the pool by adding a <code>connection-pooling-profile</code> to the connector configuration like this:</p>
<pre>
    &lt;<?cs var:class.moduleName ?>:connection-pooling-profile maxActive="10" maxIdle="10"
                             exhaustedAction="WHEN_EXHAUSTED_GROW" maxWait="120"/&gt;
</pre>
<p>The following is a list of connection attributes, each connection attribute can be configured at the config element level or
they can also be added to each processor. If they are used at the processor level they get the benefit of full expression
resolution.</p>
<table id="lconfig" class="jd-sumtable">
<tr><th colspan="12">Connection Attributes</th></tr>
<tr><th>Name</th><th>Description</th></tr>
<?cs set:count = #2 ?>
    <?cs each:connectionAttribute=class.moduleConnectVariables ?>
      <tr class="<?cs if:count % #2 ?>alt-color<?cs /if ?> api apilevel-<?cs var:field.since.key ?>" >
          <td class="jd-linkcol"><nobr><?cs var:connectionAttribute.name ?></nobr></td>
          <td class="jd-descrcol" width="100%"><?cs if:connectionAttribute.optional=="true" ?><i>Optional.&nbsp;</i><?cs /if ?><?cs call:op_tag_list(connectionAttribute.comment) ?></td>
      </tr>
      <?cs set:count = count + #1 ?>
    <?cs /each ?>
</table>
<p>Also this connector offers automatic retry for certain operations. There are a couple of situations in which a retry may solve the problem at hand, like for example if the system is
currently busy or if the session has expired. Those kind of situations are solvable by reacquiring a connection and retrying the operation.</p>
<p>By default, the connector will automatically attempt to retry the operation only once. You can at your choosing specify a greater ammount of retries by using the <i>retryMax</i>
attribute on each operation.</p>
<pre>
    ... retryMax="3"/&gt;
</pre>
<?cs /if ?>

<?cs if:class.moduleOAuthAware=="true" ?>
<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========= OAUTH DETAIL ======== -->
<h2>OAuth</h2>
<p>This connector uses OAuth as an authorization and authentication mechanism. All the message processors or sources that require the connector
to be authorized by the service provider will throw a NotAuthorizedException until the connector is authorized properly.</p>
<p>Authorizing the connector is a simple process of calling:</p>
<pre>
    &lt;<?cs var:class.moduleName ?>:authorize/&gt;
</pre>
<p>The call to authorize message processor must be made from a message coming from an HTTP inbound endpoint as the authorize process will
reply with a redirect to the service provider. The following is an example of how to use it in a flow with an HTTP inbound endpoint:</p>
<pre>
    &lt;flow name="authorizationAndAuthenticationFlow"&gt;
    &lt;http:inbound-endpoint host="localhost" port="8080" path="oauth-authorize"/&gt;
    &lt;<?cs var:class.moduleName ?>:authorize/&gt;
    &lt;/flow&gt;
</pre>
<p>If you hit that endpoint via a web-browser it will initiate the OAuth dance, redirecting the user to the service provider page
and creating a callback endpoint so the service provider can call us back once the user has been authenticated. Once the callback
gets called then the connector will switch to an authorized state and any message processor or source that requires authentication
can be called.</p>
<h3>Callback Customization</h3>
<p>As mentioned earlier once authorize gets called and before we redirect the user to the service provider we create a callback
endpoint. The callback endpoint will get called automatically by the service provider once the user is authenticated and he
grants authorization to the connector to access his private information.</p>
<p>The callback can be customized in the config element of the this connector as follows:</p>
<pre>
    &lt;<?cs var:class.moduleName ?>:config&gt;
        &lt;<?cs var:class.moduleName ?>:oauth-callback-config domain="${fullDomain}" localPort="${http.port}" remotePort="80"/&gt;
    &lt;/<?cs var:class.moduleName ?>:config&gt;
</pre>
<p>The <i>oauth-callback-config</i> element can be used to customize the endpoint that gets created for the callback. It
features the following attributes:</p>
<table id="lconfig" class="jd-sumtable">
<tr><th colspan="12">OAuth Callback Config Attributes</th></tr>
<tr><th>Name</th><th>Description</th></tr>
<tr class="api" >
<td class="jd-linkcol"><nobr>domain</nobr></td>
<td class="jd-descrcol" width="100%"><i>Optional.&nbsp;</i>The domain portion of the callback URL. This is usually something like xxx.muleion.com if you are deploying to iON for example.</td>
</tr>
<tr class="api" >
<td class="jd-linkcol"><nobr>localPort</nobr></td>
<td class="jd-descrcol" width="100%"><i>Optional.&nbsp;</i>The local port number that the endpoint will listen on. Normally 80, in the case of Mule iON you can use the environment variable ${http.port}.</td>
</tr>
<tr class="api" >
<td class="jd-linkcol"><nobr>remotePort</nobr></td>
<td class="jd-descrcol" width="100%"><i>Optional.&nbsp;</i>This is the port number that we will tell the service provider we are listening on. It is usually the same as localPort but it is separated in case your deployment features port forwarding or a proxy.</td>
</tr>
</table>
<p class="caution">The example shown above is what the configuration would look like if your app would be deployed to iON.</p>
<h3>Saving and Restoring State</h3>
<p>Once the service providers hits the callback it will do so in way that state information it is also sent. This state information
is later used by the connector on each call made to the service provider to let him know that we have completed the authorization
and authentication process.<p>
<p>The state information is currently held in-memory but the connector offers hooks to save/restore this state. The ammount of information
that needs to be saved and/or restored varies between version of the OAuth specification. At the bare minimum for OAuth 1.0a that needs
to be the OAuth access token and OAuth access token secret.</p>
<p>The following is an example of how to log the access token and access token secret.<p>
<pre>
    &lt;<?cs var:class.moduleName ?>:config&gt;
        &lt;<?cs var:class.moduleName ?>:save-oauth-access-token&gt;
            &lt;logger level="INFO" message="Received access token #[header:INBOUND:OAuthAccessToken] and #[header:INBOUND:OAuthAccessTokenSecret]"/&gt;
        &lt;/<?cs var:class.moduleName ?>:save-oauth-access-token&gt;
    &lt;/<?cs var:class.moduleName ?>:config&gt;
</pre>
<p>The <i>save-oauth-access-token</i> is a message processor chain and you can add inside of it as many message processors as you want. The chain will
be called with special inbound properties in the message which the information that needs saved.</p>
<p>Restoring the information is equally simple:</p>
<pre>
    &lt;<?cs var:class.moduleName ?>:config&gt;
        &lt;<?cs var:class.moduleName ?>:restore-oauth-access-token&gt;
            &lt;message-properties-transformer&gt;
                &lt;add-message-property key="OAuthAccessToken" value="123"/&gt;
                &lt;add-message-property key="OAuthAccessTokenSecret" value="567"/&gt;
            &lt;/message-properties-transformer&gt;
        &lt;/<?cs var:class.moduleName ?>:restore-oauth-access-token&gt;
    &lt;/<?cs var:class.moduleName ?>:config&gt;
</pre>
<p>The example above does not do anything useful expect hardcode the access token and the token secret to 123 and 567 respectively. Just like
<i>save-oauth-access-token</i>, <i>restore-oauth-access-token</i> is another message processor chain. Once the chain is done executing
we will extract the OAuth access token property and OAuth access token secret property values.</p>
<p>The following shows a full example on how to save and restore using Mule ObjectStore Module to store the information inside an object store.<p>
<pre>
&lt;mule xmlns="http://www.mulesoft.org/schema/mule/core"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:<?cs var:class.moduleName ?>="<?cs var:class.moduleNamespace ?>"
      xmlns:objectstore="http://www.mulesoft.org/schema/mule/objectstore"
      xsi:schemaLocation="
               http://www.mulesoft.org/schema/mule/core
               http://www.mulesoft.org/schema/mule/core/current/mule.xsd
               http://www.mulesoft.org/schema/mule/objectstore
               http://www.mulesoft.org/schema/mule/objectstore/1.0/mule-objectstore.xsd
               <?cs var:class.moduleNamespace ?>
               <?cs var:class.moduleSchemaLocation ?>"&gt;

    &lt;<?cs var:class.moduleName ?>:config&gt;
        &lt;<?cs var:class.moduleName ?>:save-oauth-access-token&gt;
            &lt;objectstore:store key="OAuthAccessToken" value="#[header:INBOUND:OAuthAccessToken]"/&gt;
            &lt;objectstore:store key="OAuthAccessTokenSecret" value="#[header:INBOUND:OAuthAccessTokenSecret]"/&gt;
        &lt;/<?cs var:class.moduleName ?>:save-oauth-access-token&gt;
        &lt;<?cs var:class.moduleName ?>:restore-oauth-access-token&gt;
            &lt;enricher target="#[header:OAuthAccessToken]"&gt;
                &lt;objectstore:retrieve key="OAuthAccessToken"/&gt;
            &lt;/enricher&gt;
            &lt;enricher target="#[header:OAuthAccessTokenSecret]"&gt;
                &lt;objectstore:retrieve key="OAuthAccessTokenSecret"/&gt;
            &lt;/enricher&gt;
        &lt;/<?cs var:class.moduleName ?>:restore-oauth-access-token&gt;
    &lt;/<?cs var:class.moduleName ?>:config&gt;

&lt;/mule&gt;
</pre>

<?cs /if ?>

<?cs def:write_op_details(methods) ?>
<?cs each:method=methods ?>
<?cs # the A tag in the next line must remain where it is, so that Eclipse can parse the docs ?>
<A NAME="<?cs var:method.elementName ?>"></A>
<?cs # The apilevel-N class MUST BE LAST in the sequence of class names ?>
<div class="jd-details api apilevel-<?cs var:method.since.key ?>">
    <h4 class="jd-details-title">
      <span class="sympad">&lt;<?cs var:class.moduleName ?>:<?cs var:method.elementName ?>&gt;</span>
    </h4>
      <div class="api-level">
        <div><?cs call:since_tags(method) ?></div>
      </div>
    <div class="jd-details-descr">
      <?cs call:op_description(method) ?>
    </div>
</div>
<?cs /each ?>
<?cs /def ?>

<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========= MESSAGE PROCESSOR DETAIL ======== -->
<!-- Message Processors -->
<?cs if:subcount(class.methods.processor) ?>
<h2>Message Processors</h2>
<?cs call:write_op_details(class.methods.processor) ?>
<?cs /if ?>

<?cs # this next line must be exactly like this to be parsed by eclipse ?>
<!-- ========= MESSAGE SOURCES DETAIL ======== -->
<!-- Message Processors -->
<?cs if:subcount(class.methods.source) ?>
<h2>Message Sources</h2>
<?cs call:write_op_details(class.methods.source) ?>
<?cs /if ?>


<?cs # the next two lines must be exactly like this to be parsed by eclipse ?>
<!-- ========= END OF CLASS DATA ========= -->
<A NAME="navbar_top"></A>

<?cs include:"footer.cs" ?>
</div> <!-- jd-content -->

</div><!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>
