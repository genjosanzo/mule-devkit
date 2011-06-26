<?cs def:custom_masthead() ?>
<div id="header">
    <div id="headerLeft">
    <?cs if:project.name ?>
      <span id="masthead-title"><?cs var:project.name ?></span>
    <?cs /if ?>
    <ul id="header-tabs" class="<?cs var:section ?>">
    	<li id="guide"><a href="<?cs var:toroot ?>../guide/index.html">
	    	<span class="en">User Guide</span>
    	</a></li>
    	<li id="java"><a href="<?cs var:toroot ?>../java/index.html">
	    	<span class="en">Java API</span>
    	</a></li>
    	<li id="mule"><a href="<?cs var:toroot ?>../mule/index.html">
	    	<span class="en">Mule XML</span>
    	</a></li>
    </ul>
    </div>
    <div id="headerRight">
      <a href="http://www.mulesoft.com" tabindex="-1">
      <img src="<?cs var:toassets ?>/images/mulesoft-logo-final.gif" alt="MuleSoft" border="0" id="mulesoftlogo"></a><br/>
      <?cs call:default_search_box() ?>
      <?cs if:reference && reference.apilevels ?>
        <?cs call:default_api_filter() ?>
      <?cs /if ?>
    </div>
</div><!-- header -->
<?cs /def ?>