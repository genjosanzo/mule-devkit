<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="gc-documentation">
<?cs call:custom_masthead() ?>

<div class="g-section g-tpl-240" id="body-content">

	<div class="g-unit g-first side-nav-resizable" id="side-nav">
	      <div id="swapper">
	        <div id="side-nav">
                <script type="text/javascript" charset="utf-8">
                    $(document).ready(function(){
                        $("ul#toc").tableOfContents();
                        buildToggleLists();
                    })
                </script>
                <ul id="toc">
                </ul>
	        </div><!-- end nav-panels -->
	      </div><!-- end swapper -->
	    </div> <!-- end side-nav -->

<div class="g-unit" id="doc-content">

<div id="jd-content">

<?cs var:content ?>

<?cs include:"footer.cs" ?>
</div><!-- end jd-content -->
</div> <!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>
