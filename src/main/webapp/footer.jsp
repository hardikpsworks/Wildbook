<%@ page
		contentType="text/html; charset=utf-8"
		language="java"
     	import="org.ecocean.CommonConfiguration,
      org.ecocean.ContextConfiguration,
      org.ecocean.ShepherdProperties,
      org.ecocean.servlet.ServletUtilities,
      org.ecocean.Shepherd,
      org.ecocean.User,
      java.util.ArrayList,
      java.util.List,
      java.util.Properties,
      org.apache.commons.text.WordUtils,
      org.ecocean.security.Collaboration
      "
%>
        <%
String context="context0";
context=ServletUtilities.getContext(request);
String langCode=ServletUtilities.getLanguageCode(request);
Properties props = new Properties();
props = ShepherdProperties.getProperties("footer.properties", langCode, context);


String urlLoc = "//" + CommonConfiguration.getURLLocation(request);


        %>

        <!-- footer -->

        <footer class="page-footer" >

		<hr>

            <div class="container-fluid" style="background-color: white;" >
              <div class="container wide main-section">

			  	<div class="row">
				     <!-- Your Company Name -->
					<a href="http://www.ncaquariums.com" target="_blank"> <img src="<%=urlLoc %>/images/ncaquariums/footerlogo1.jpg" alt="NC Aquarium"> </a>
					<a href="http://www.sezarc.org" target="_blank"> <img src="<%=urlLoc %>/images/ncaquariums/footerlogo2.jpg" target="_blank" alt="SEZARK"> </a>
					<a href="https://www.blueelementsimaging.com" target="_blank"> <img src="<%=urlLoc %>/images/ncaquariums/footerlogo3.jpg" alt="Blue Elements Imaging"> </a>
					<a href="https://www.wildme.org" target="_blank"> <img src="<%=urlLoc %>/images/WildMe-Logo-04.png" alt="Wild Me" style="height: 120px;"> </a>
					<a href="http://mnzoo.org" target="_blank"> <img src="<%=urlLoc %>/images/ncaquariums/footerlogo5.jpg" alt="MN Zoo"> </a>
					<a href="https://www.georgiaaquarium.org/" target="_blank"> <img src="<%=urlLoc %>/images/ncaquariums/footerlogo6.jpg" alt="Georgia Aquarium"> </a>
					<a href="https://www.coastalstudiesinstitute.org/" target="_blank"> <img src="<%=urlLoc %>/images/ncaquariums/coastalStudiesInstitute.jpg" alt="Coastal Studies Institute"> </a>
          <a href="http://www.wildbook.org" target="_blank"> <img src="<%=urlLoc %>/images/WildBook_logo_72dpi-01.png" alt="Wildbook logo" class="" style="height: 120px;"/></a>
					<!-- Copyright -->
				</div>

				<div class="row">
					<p class="col-sm-8" style="margin-top:30px;">For more information contact <a href="mailto:spotasharkusa@gmail.com">Spot A Shark USA</a>.</p>
          </p>
          <div class="row">
						<p class="col-sm-8" style="margin-top:10px;"><a href="https://www.wildme.org/#/wildbook">Wildbook v.<%=ContextConfiguration.getVersion() %></a> is distributed under the GPL v2 license and is intended to support mark-recapture field studies. <a href="http://ncaquariums.wildbook.org/userAgreement.jsp" target="_blank">Use of this site is governed by our User Agreement.</a>
            </p>
        	</div>
        </div>
      </div>
    </div>

  </footer>
        <!-- /footer -->
</body>
</html>
