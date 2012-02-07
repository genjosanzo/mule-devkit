<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<?cs if:project.name ?>
  <meta name="description" content="Documentation for <?cs var:project.name ?>." />
<?cs else ?>
  <meta name="description" content="Documentation." />
<?cs /if ?>
<link rel="shortcut icon" type="image/x-icon" href="<?cs var:toroot ?>favicon.ico" />
<title>
<?cs if:page.title ?>
  <?cs var:page.title ?>
<?cs /if ?>
<?cs if:project.name ?>
| <?cs var:project.name ?>
<?cs /if ?>
</title>
<link href="<?cs var:toassets ?>mule-developer-docs.css" rel="stylesheet" type="text/css" />
<link href="<?cs var:toassets ?>mule-developer-prettify.css" rel="stylesheet" type="text/css" />
<link href="<?cs var:toassets ?>customizations.css" rel="stylesheet" type="text/css" />
<link href='http://fonts.googleapis.com/css?family=Ubuntu:300' rel='stylesheet' type='text/css'>
<script src="<?cs var:toassets ?>search_autocomplete.js" type="text/javascript"></script>
<script src="<?cs var:toassets ?>jquery-resizable.min.js" type="text/javascript"></script>
<script src="<?cs var:toassets ?>mule-developer-docs.js" type="text/javascript"></script>
<script src="<?cs var:toassets ?>prettify.js" type="text/javascript"></script>
<script type="text/javascript">
  setToRoot("<?cs var:toroot ?>", "<?cs var:toassets ?>");
</script><?cs 
if:reference ?>
<script src="<?cs var:toassets ?>mule-developer-reference.js" type="text/javascript"></script>
<script src="<?cs var:toassets ?>navtree_data.js" type="text/javascript"></script><?cs 
/if ?>
<script src="<?cs var:toassets ?>customizations.js" type="text/javascript"></script>
<noscript>
  <style type="text/css">
    html,body{overflow:auto;}
    #body-content{position:relative; top:0;}
    #doc-content{overflow:visible;border-left:3px solid #666;}
    #side-nav{padding:0;}
    #side-nav .toggle-list ul {display:block;}
    #resize-packages-nav{border-bottom:3px solid #666;}
  </style>
</noscript>
<script type="text/javascript" charset="utf-8">
  var is_ssl = ("https:" == document.location.protocol);
  var asset_host = is_ssl ? "https://s3.amazonaws.com/getsatisfaction.com/" : "http://s3.amazonaws.com/getsatisfaction.com/";
  document.write(unescape("%3Cscript src='" + asset_host + "javascripts/feedback-v2.js' type='text/javascript'%3E%3C/script%3E"));
</script>

<script type="text/javascript" charset="utf-8">
  var feedback_widget_options = {};

  feedback_widget_options.display = "overlay";
  feedback_widget_options.company = "mulesoft";
  feedback_widget_options.placement = "right";
  feedback_widget_options.color = "#2F74AE";
  feedback_widget_options.style = "question";

  feedback_widget_options.tag = "<?cs var:project.name ?>";

  feedback_widget_options.limit = "3";

  var feedback_widget = new GSFN.feedback_widget(feedback_widget_options);
</script>
</head>
