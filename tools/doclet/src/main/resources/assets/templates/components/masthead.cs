<?cs def:custom_masthead() ?>
<div id="header">
    <div id="headerLeft">
    <?cs if:project.name ?>
      <span id="masthead-title"><?cs var:project.name ?></span>
    <?cs /if ?>
    <ul id="header-tabs" class="reference">
    	<li id="home-link"><a href="/index.html">
	    	<span class="en">Home</span>
    	</a></li>
    	<li id="java-reference"><a href="/reference/java/packages.html">
	    	<span class="en">Java API</span>
    	</a></li>
    	<li id="mule-xml-reference"><a href="/reference/mule/xml/index.html">
	    	<span class="en">Mule XML</span>
    	</a></li>
    	<li id="mule-dsl-reference"><a href="/reference/mule/dsl/index.html">
	    	<span class="en">Mule DSL</span>
    	</a></li>
    </div>
    <div id="headerRight">
      <?cs call:default_search_box() ?>
      <?cs if:reference && reference.apilevels ?>
        <?cs call:default_api_filter() ?>
      <?cs /if ?>
    </div>
</div><!-- header -->
<?cs /def ?>