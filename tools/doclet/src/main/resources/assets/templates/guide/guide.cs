<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="gc-documentation">
<?cs call:custom_masthead() ?>
<?cs call:custom_home_nav() ?>

<div class="g-unit" id="doc-content" style="margin-left: 300px;">

<div id="jd-header">
<h1>Installation</h1>
</div>

<div id="jd-content">
<div class="jd-descr">
<p>The connector can either be installed for all applications running within the Mule instance or can be setup to be
used for a single application.</p>

<h2>All Applications</h2>

<p>Download the connector from the link above and place the resulting jar file in /lib/user directory of the Mule
installation folder.</p>

<h2>Single Application</h2>

<p>To make the connector available only to single application then place it in the lib directory of the application
otherwise if using Maven to compile and deploy your application the following can be done:</p>

<p>Add the module's Maven repo to your pom.xml:</p>

<pre>
&lt;repositories&gt;
    &lt;repository&gt;
        &lt;id&gt;muleforge-releases&lt;/id&gt;
        &lt;name&gt;MuleForge Snapshot Repository&lt;/name&gt;
        &lt;url&gt;https://repository.mulesoft.org/releases/&lt;/url&gt;
        &lt;layout&gt;default&lt;/layout&gt;
    &lt;/repsitory&gt;
&lt;/repositories&gt;
</pre>

<p>Add the connector as a dependency to your project. This can be done by adding the following under the dependencies
element in the pom.xml file of the application:</p>

<pre>
&lt;dependency&gt;
    &lt;groupId&gt;org.mule.modules&lt;/groupId&gt;
    &lt;artifactId&gt;mule-module-jira&lt;/artifactId&gt;
    &lt;version&gt;1.0&lt;/version&gt;
&lt;/dependency&gt;
</pre>
</div>
</div><!-- end jd-content -->

<?cs include:"footer.cs" ?>
</div> <!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>
