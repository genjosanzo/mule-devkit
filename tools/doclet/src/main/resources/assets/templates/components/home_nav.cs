<?cs # The default side navigation for the reference docs ?><?cs
def:custom_home_nav() ?>
 <div class="g-section g-tpl-240" id="body-content">
    <div class="g-unit g-first side-nav-resizable" id="side-nav">
      <div id="devdoc-nav"><ul>
  <li>
    <h2><span class="en">Getting Started</span>
    </h2>
    <ul>
      <li class="toggle-list">
        <div><a href="guide/installation.html">
            <span class="en">Installation</span>
          </a></div>
        <ul id="devdoc-nav-sample-list">
          <li><a href="guide/installation/maven.html">
                <span class="en">Single Application</span>
              </a></li>
          <li><a href="guide/installation/maven.html">
                <span class="en">All Applications</span>
              </a></li>
        </ul>
      </li>
      <li class="toggle-list">
        <div><a href="/resources/browser.html?tag=article">
               <span class="en">Articles</span>
             </a></div>
        <ul id="devdoc-nav-article-list">
        </ul>
      </li>
    </ul>
  </li>
  </div> <!-- devdoc-nav -->
  </div>
  </div>





    <script>
      if (!isMobile) {
        $("<a href='#' id='nav-swap' onclick='swapNav();return false;' style='font-size:10px;line-height:9px;margin-left:1em;text-decoration:none;'><span id='tree-link'>Use Tree Navigation</span><span id='panel-link' style='display:none'>Use Panel Navigation</span></a>").appendTo("#side-nav");
        chooseDefaultNav();
        if ($("#nav-tree").is(':visible')) {
          init_default_navtree("<?cs var:toroot ?>");
        } else {
          addLoadEvent(function() {
            scrollIntoView("packages-nav");
            scrollIntoView("classes-nav");
          });
        }
        $("#swapper").css({borderBottom:"2px solid #aaa"});
      } else {
        swapNav(); // tree view should be used on mobile
      }
    </script><?cs
/def ?>