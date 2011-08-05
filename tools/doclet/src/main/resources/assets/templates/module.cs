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
    <?cs var:project.version ?>

</div><!-- end header -->

<div id="naMessage"></div>

<div id="jd-content" class="api apilevel-<?cs var:class.since.key ?>">
<?cs # this next line must be exactly like this to be parsed by eclipse ?>

<div class="jd-tagdata">
      <table class="jd-tagtable">
        <tbody><tr>
          <th>Namespace</th><td><?cs var:class.moduleNamespace ?></td>
        </tr><tr>
          <th>Schema Location</th><td><?cs var:class.moduleSchemaLocation ?></td>
        </tr>
<tr>
          <th>Version</th><td><?cs var:class.moduleVersion ?></td>
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
        <span class="sympad">&lt;<?cs var:class.moduleName ?>:config&gt;</nobr>
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
<!-- ========= FIELD DETAIL ======== -->
<h2>Configuration</h2>
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
<?cs /if ?>
</table>

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
