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

					<ul>

					  <li>
					    <span class="heading">
					      <span>Title 1</span>
					    </span>
					    <ul>
					      <li><a href="title-2.html">
					        <span>Title 2</span></a></li>
					      <li><a href="title-3.html">
					        <span>Title 3</span></a></li>
					      <li><a href="title-4.html">
					        <span>Title 4</span></a></li>
					    </ul>
					  </li>
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
