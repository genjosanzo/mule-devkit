<?cs def:custom_masthead() ?>
<div id="header">
    <div id="headerLeft">
    <?cs if:project.name ?>
      <span id="masthead-title"><?cs var:project.name ?></span>
    <?cs /if ?>
    <ul id="header-tabs" class="<?cs var:section ?>">
    <?cs each:tab=tabs ?>
    	<li id="<?cs var:tab.id ?>"><a href="<?cs var:toroot ?><?cs var:tab.link ?>">
	    	<span class="en"><?cs var:tab.title ?></span>
    	</a></li>
    <?cs /each ?>
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