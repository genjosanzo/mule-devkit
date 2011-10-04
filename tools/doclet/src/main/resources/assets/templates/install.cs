<?cs set:section = "guide" ?>
<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="gc-documentation">
<?cs call:custom_masthead() ?>

<div class="g-unit" id="all-content">

<div id="jd-header">
<h1><?cs var:page.title ?></h1>
</div>

<div id="jd-content">

<p>This module can be used in several different ways depending on your needs and requirements. The following sections
details how to use and install this module under different scenarios / environments.</p>

<h2>As a Shared Mule Module</h2>

<p>Download the connector from the MuleForge and place the resulting ZIP file in /plugins directory of the Mule installation folder. For more
information about how the Mule Plugin System works see <a href="http://www.mulesoft.org/documentation/display/MULE3USER/Classloader+Control+in+Mule">this link</a>.</p>

<h2>As a Maven Dependency</h2>

<p>To make the module available to a Mavenized Mule application, first add the following repositories to your POM:</p>

<pre>
&lt;repositories&gt;
    &lt;repository&gt;
        &lt;id&gt;<?cs var:project.repo.id ?>&lt;/id&gt;
        &lt;name&gt;<?cs var:project.repo.name ?>&lt;/name&gt;
        &lt;url&gt;<?cs var:project.repo.url ?>&lt;/url&gt;
        &lt;layout&gt;default&lt;/layout&gt;
    &lt;/repository&gt;
    &lt;repository&gt;
        &lt;id&gt;<?cs var:project.snapshotRepo.id ?>&lt;/id&gt;
        &lt;name&gt;<?cs var:project.snapshotRepo.name ?>&lt;/name&gt;
        &lt;url&gt;<?cs var:project.snapshotRepo.url ?>&lt;/url&gt;
        &lt;layout&gt;default&lt;/layout&gt;
    &lt;/repository&gt;
&lt;/repositories&gt;
</pre>

Then add the module as a dependency to your project. This can be done by adding the following under the dependencies
element your POM:

<pre>
&lt;dependency&gt;
    &lt;groupId&gt;<?cs var:project.groupId ?>&lt;/groupId&gt;
    &lt;artifactId&gt;<?cs var:project.artifactId ?>&lt;/artifactId&gt;
    &lt;version&gt;<?cs var:project.version ?>&lt;/version&gt;
&lt;/dependency&gt;
</pre>

If you plan to use this module inside a Mule application, you need add it to the packaging process. That way the final
ZIP file which will contain your flows and Java code will also contain this module and its dependencies.

Add an special inclusion to the configuration of the Mule Maven plugin for this module as follows:

<pre>
&lt;plugin&gt;
    &lt;groupId&gt;org.mule.tools&lt;/groupId&gt;
    &lt;artifactId&gt;maven-mule-plugin&lt;/artifactId&gt;
    &lt;extensions&gt;true&lt;/extensions&gt;
    &lt;configuration&gt;
        &lt;excludeMuleDependencies&gt;false&lt;/excludeMuleDependencies&gt;
        &lt;inclusions&gt;
            &lt;inclusion&gt;
                &lt;groupId&gt;<?cs var:project.groupId ?>&lt;/groupId&gt;
                &lt;artifactId&gt;<?cs var:project.artifactId ?>&lt;/artifactId&gt;
            &lt;/inclusion&gt;
        &lt;/inclusions&gt;
    &lt;/configuration&gt;
&lt;/plugin&gt;
</pre>

<?cs include:"footer.cs" ?>
</div><!-- end jd-content -->
</div> <!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>
