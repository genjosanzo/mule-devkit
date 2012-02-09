<?cs set:section = "mule" ?>
<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="<?cs var:class.since.key ?>">
<?cs call:custom_masthead() ?>

<div class="g-unit" id="all-content">

<div id="api-info-block">

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
<h2>Schema</h2>
<pre><?cs var:class.moduleSchema ?></pre>
</div><!-- jd-descr -->

<?cs include:"footer.cs" ?>
</div> <!-- jd-content -->

</div><!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>
