<?cs set:section = "mule" ?>
<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="gc-documentation">
<?cs call:custom_masthead() ?>
<?cs # The default side navigation for the reference docs ?><?cs
def:custom_left_nav() ?>
  <div class="g-section g-tpl-240" id="body-content">
    <div class="g-unit g-first side-nav-resizable" id="side-nav">
      <div id="swapper">
        <div id="nav-panels">
          <div id="classes-nav">
              <p style="padding:10px">Select a module to view its members</p><br/>
          </div><!-- end classes -->
        </div><!-- end nav-panels -->
      </div><!-- end swapper -->
    </div> <!-- end side-nav -->
    <?cs
/def ?>

<div class="g-unit" id="doc-content">

<div id="jd-header">
<h1><?cs var:page.title ?></h1>
</div>

<div id="jd-content">

<div class="jd-descr">
<p><?cs call:tag_list(root.descr) ?></p>
</div>

<?cs set:count = #1 ?>
<table class="jd-sumtable">
<?cs each:pkg = docs.packages ?>
    <?cs each:mod = pkg.modules ?>
    <tr class="<?cs if:count % #2 ?>alt-color<?cs /if ?> api" >
        <td class="jd-linkcol"><?cs call:module_link(mod) ?></td>
        <td class="jd-descrcol"><?cs var:mod.namespace ?></td>
        <td class="jd-descrcol" width="100%"><?cs call:tag_list(mod.shortDescr) ?></td>
    </tr>
    <?cs /each ?>
<?cs set:count = count + #1 ?>
<?cs /each ?>
</table>

<?cs include:"footer.cs" ?>
</div><!-- end jd-content -->
</div> <!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>
