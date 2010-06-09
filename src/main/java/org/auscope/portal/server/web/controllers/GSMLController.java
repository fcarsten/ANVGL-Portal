package org.auscope.portal.server.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.auscope.portal.server.web.service.HttpServiceCaller;
import org.auscope.portal.server.web.view.JSONModelAndView;
import org.auscope.portal.server.util.GmlToKml;
import org.auscope.portal.csw.ICSWMethodMaker;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * Acts as a proxy to WFS's
 *
 * User: Mathew Wyatt
 * @version $Id$
 */

@Controller
public class GSMLController {
    protected final Log logger = LogFactory.getLog(getClass().getName());
    private HttpServiceCaller serviceCaller;
    private GmlToKml gmlToKml;

    @Autowired
    public GSMLController(HttpServiceCaller serviceCaller,
                          GmlToKml gmlToKml) {
        this.serviceCaller = serviceCaller;
        this.gmlToKml = gmlToKml;
    }

    /**
     * Given a service Url and a feature type this will query for all of the features, then convert them into KML,
     * to be displayed, assuming that the response will be complex feature GeoSciML
     *
     * @param serviceUrl
     * @param featureType
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping("/getAllFeatures.do")
    public ModelAndView requestAllFeatures(@RequestParam("serviceUrl") final String serviceUrl,
                                           @RequestParam("typeName") final String featureType,
                                           HttpServletRequest request) throws Exception {

        String gmlResponse = 
            serviceCaller.getMethodResponseAsString(new ICSWMethodMaker() {
                public HttpMethodBase makeMethod() {
                    GetMethod method = new GetMethod(serviceUrl);
    
                    //set all of the parameters
                    NameValuePair request = new NameValuePair("request", "GetFeature");
                    NameValuePair elementSet = new NameValuePair("typeName", featureType);
    
                    //attach them to the method
                    method.setQueryString(new NameValuePair[]{request, elementSet});
    
                    return method;
                }
            }.makeMethod(), serviceCaller.getHttpClient());

        return makeModelAndViewKML(gmlToKml.convert(gmlResponse, request, serviceUrl), gmlResponse);
    }
    
    /**
     * Given a service Url, a feature type and a specific feature ID, this function will fetch the specific feature and 
     * then convert it into KML to be displayed, assuming that the response will be complex feature GeoSciML
     * @param serviceUrl
     * @param featureType
     * @param featureId
     * @param request
     * @return
     */
    @RequestMapping("/requestFeature.do")
    public ModelAndView requestFeature(@RequestParam("serviceUrl") final String serviceUrl,
            						   @RequestParam("typeName") final String featureType,
            						   @RequestParam("featureId") final String featureId,
            						   HttpServletRequest request) throws Exception {
    	String gmlResponse = serviceCaller.getMethodResponseAsString(new ICSWMethodMaker() {
            public HttpMethodBase makeMethod() {
                GetMethod method = new GetMethod(serviceUrl);

                ArrayList<NameValuePair> valuePairs = new ArrayList<NameValuePair>();
                
                //set all of the parameters
                valuePairs.add(new NameValuePair("request", "GetFeature"));
                valuePairs.add(new NameValuePair("typeName", featureType));
                valuePairs.add(new NameValuePair("featureId", featureId));
                

                //attach them to the method
                method.setQueryString((NameValuePair[]) valuePairs.toArray(new NameValuePair[valuePairs.size()]));

                return method;
            }
        }.makeMethod(), serviceCaller.getHttpClient());

        return makeModelAndViewKML(gmlToKml.convert(gmlResponse, request, serviceUrl), gmlResponse);
    }

    @RequestMapping("/xsltRestProxy.do")
    public void xsltRestProxy(@RequestParam("serviceUrl") String serviceUrl,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        try {
            String result = serviceCaller.getMethodResponseAsString(new GetMethod(serviceUrl), serviceCaller.getHttpClient());

            // Send response back to client
            response.getWriter().println(gmlToKml.convert(result, request, serviceUrl));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Insert a kml block into a successful JSON response
     * @param kmlBlob
     * @return
     */
    private ModelAndView makeModelAndViewKML(final String kmlBlob, final String gmlBlob) {

        final Map<String,String> data = new HashMap<String,String>();
        data.put("kml", kmlBlob);
        data.put("gml", gmlBlob);

        
        ModelMap model = new ModelMap();
        model.put("success", true);
        model.put("data", data);

        return new JSONModelAndView(model);
    }
    
    /**
     * Utility method specific to Auscope Portal
     * @param geoXML
     * @param httpRequest
     *
   public String convert(String geoXML, HttpServletRequest httpRequest) {
      InputStream inXSLT = httpRequest.getSession().getServletContext().getResourceAsStream("/WEB-INF/xsl/kml.xsl");      
      return this.convert(geoXML, inXSLT);
   }
   */
}
