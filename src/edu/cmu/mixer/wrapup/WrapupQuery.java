package edu.cmu.mixer.wrapup;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.json.JSONArray;

@SuppressWarnings("serial")
public class WrapupQuery extends javax.servlet.http.HttpServlet {

  private static final Logger logger = Logger.getLogger(WrapupQuery.class.getName());

  public void doGet(javax.servlet.http.HttpServletRequest req,
                    javax.servlet.http.HttpServletResponse resp)
  throws java.io.IOException {
    this.doPost(req, resp);
  }

  public void doPost(javax.servlet.http.HttpServletRequest req,
                     javax.servlet.http.HttpServletResponse resp)
  throws java.io.IOException {

    String xo = req.getParameter("xo");
    if (xo != null) {
      resp.setHeader("Access-Control-Allow-Origin", "*");
      resp.setHeader("Access-Control-Allow-Methods",
		     "POST, GET, OPTIONS, DELETE");
      resp.setHeader("Access-Control-Max-Age", "3600");
      resp.setHeader("Access-Control-Allow-Headers", "x-requested-with");
    }

    try {
      String callback = req.getParameter("callback");

      org.json.JSONArray wrappers = new JSONArray();
      org.json.JSONObject json = new JSONObject();

      json.putOpt("url", req.getParameter("url"));
      json.putOpt("wrappers", wrappers);
      System.err.println("QUERY0: " + json.toString());


      if (req.getParameter("signature") != null) {
        org.json.JSONArray bysig = queryBySignature(req.getParameter("signature"));
        for (int i = 0; i < bysig.length(); i++) {
          wrappers.put(bysig.opt(i));
        }
      }

      if (req.getParameter("url") != null) {
        org.json.JSONArray byurl = queryByURL(req.getParameter("url"));
        for (int i = 0; i < byurl.length(); i++) {
          wrappers.put(byurl.opt(i));
        }

	if (wrappers.length() == 0) {
	  // try www.HOSTNAME
	  java.net.URL purl = new java.net.URL(req.getParameter("url"));
	  if (! purl.getHost().startsWith("www.")) {
	    purl = new java.net.URL(purl.getProtocol(),
				    "www." + purl.getHost(),
				    purl.getPort(),
				    purl.getPath());
	    byurl = queryByURL(purl, req.getParameter("url"));
	    for (int i = 0; i < byurl.length(); i++) {
	      wrappers.put(byurl.opt(i));
	    }
	    
	  }
	}
      }
      System.err.println("QUERY1: " + json.toString());

      if (wrappers.length() == 0) {
        logger.log(Level.WARNING, "No wrapper is retrieved.");
        resp.setStatus(javax.servlet.http.HttpServletResponse.SC_NO_CONTENT);
	return;
      }

      logger.log(Level.INFO, "#WRAP: " + wrappers.length());

      if (callback == null) {
        resp.setContentType("application/json");
        resp.getWriter().write(json.toString(2));
      } else {
        resp.setContentType("application/javascript");
        resp.getWriter().write(callback);
        resp.getWriter().write("(");
        resp.getWriter().write(json.toString(2));
        resp.getWriter().write(");\n");
      }
    } catch (Exception ex) {
      resp.setContentType("text/plain");
      ex.printStackTrace(resp.getWriter());
      ex.printStackTrace(System.err);
    }
  }

  protected static com.google.appengine.api.datastore.DatastoreService ds =
    com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService();

  // TODO (songminz): Also check nested wrappers.
  private static boolean isWrapperValid(JSONArray wrappers) {
    for (int i = 0; i < wrappers.length(); i++) {
      JSONObject wrapper = wrappers.optJSONObject(i);
      String parentXpathSelector = wrapper.optString("xpathSelector");

      if (parentXpathSelector.equals("//null")) {
        logger.log(Level.SEVERE,
                   String.format("Invalid parent xpathSelector %s for wrapper: %s",
                                 parentXpathSelector, wrapper.toString()));
        return false;
      }

      try {
        JSONObject attributes = wrapper.getJSONObject("attributes");
        java.util.Iterator<?> attributeIterator = attributes.keys();
        while (attributeIterator.hasNext()) {
          String key = (String) attributeIterator.next();
          String childXpathSelector =
            attributes.getJSONObject(key).getString("xpathSelector");
          if (childXpathSelector.equals("./null")) {
            logger.log(Level.SEVERE,
                       String.format("Invalid child xpathSelector %s for wrapper: %s",
                                     childXpathSelector, wrapper.toString()));
            return false;
          }
        }
      } catch (org.json.JSONException e) {
        e.printStackTrace(System.err);
        return false;
      }
    }
    return true;
  }

  public static org.json.JSONArray queryBySignature(String signature) {
    // PLACEHOLDER; NOT YET IMPLEMENTED
    return new org.json.JSONArray();
  }
  public static org.json.JSONArray queryByURL(String url) throws Exception {
    return queryByURL(new java.net.URL(url), url);    
  }
  public static org.json.JSONArray queryByURL(java.net.URL purl, String url) {  
    org.json.JSONArray results = new JSONArray();

    org.json.JSONObject urlparts = new org.json.JSONObject();
    try {
      urlparts.putOpt("hostname", purl.getHost());
      urlparts.putOpt("pathname", purl.getPath());
    } catch (Exception ex) {
      urlparts = null;
    }

    System.err.println("BYURL: " + urlparts.toString());
    
    com.google.appengine.api.datastore.Query.Filter filter = null;
    if ((urlparts != null) && (urlparts.length() > 0)) {
      filter = com.google.appengine.api.datastore.Query.CompositeFilterOperator.or(
										   com.google.appengine.api.datastore.Query.CompositeFilterOperator.and(
																			//com.google.appengine.api.datastore.Query.FilterOperator.NOT_EQUAL.of("hostname", null),
																			//com.google.appengine.api.datastore.Query.FilterOperator.NOT_EQUAL.of("pathname", null),
																			com.google.appengine.api.datastore.Query.FilterOperator.EQUAL.of("hostname", urlparts.optString("hostname")),
																			com.google.appengine.api.datastore.Query.FilterOperator.EQUAL.of("pathname", urlparts.optString("pathname"))),
										   com.google.appengine.api.datastore.Query.FilterOperator.EQUAL.of("url", url));	   

      /*
      filter =
	new com.google.appengine.api.datastore.Query.CompositeFilter(com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND,
								     java.util.Arrays.asList(
											     new com.google.appengine.api.datastore.Query.FilterPredicate("hostname",
																			  com.google.appengine.api.datastore.Query.FilterOperator.EQUAL,
																			  urlparts.optString("hostname")),
											     new com.google.appengine.api.datastore.Query.FilterPredicate("pathname",
																			  com.google.appengine.api.datastore.Query.FilterOperator.EQUAL,
																			  urlparts.optString("pathname"))
											     ));
      */
    } else {
      // literal matching by url
      filter = new com.google.appengine.api.datastore.Query.FilterPredicate("url",
									    com.google.appengine.api.datastore.Query.FilterOperator.EQUAL,
									    url);
    }

    
    com.google.appengine.api.datastore.FetchOptions fo = 
      com.google.appengine.api.datastore.FetchOptions.Builder.withDefaults();
    com.google.appengine.api.datastore.Query query =
      new com.google.appengine.api.datastore.Query("Wrapper");
    query = query.setFilter(filter);
    query = query.addSort("downVotes",
			  com.google.appengine.api.datastore.Query.SortDirection.DESCENDING);    
    query = query.addSort("diameter",
			  com.google.appengine.api.datastore.Query.SortDirection.DESCENDING);    
    query = query.addSort("upVotes",
			  com.google.appengine.api.datastore.Query.SortDirection.DESCENDING);    
    query = query.addSort("timestamp",
			  com.google.appengine.api.datastore.Query.SortDirection.DESCENDING);    
    
    com.google.appengine.api.datastore.PreparedQuery pq =
      ds.prepare(query);
    for (com.google.appengine.api.datastore.Entity result : pq.asIterable(fo)) {
      if (! result.hasProperty("wrapper")) {
	continue;
      }

      
      String wrapperString = ((com.google.appengine.api.datastore.Text) result.getProperty("wrapper")).getValue();
      JSONArray wrapper = new JSONArray();
      try {
	wrapper = new JSONArray(wrapperString);
	for (int i = 0; i < wrapper.length(); i++) {
	  wrapper.optJSONObject(i).putOpt("wrapperName", result.getKey().getName());
	}
      } catch (Exception ex) {
	try {
	  org.json.JSONObject obj = new org.json.JSONObject(wrapperString);
	  obj.putOpt("wrapperName", result.getKey().getName());
	  wrapper.put(obj);
	} catch (Exception ex2) {
	  ex2.printStackTrace(System.err);
	}
      }

      if (wrapper.length() > 0) {
	if (! isWrapperValid(wrapper)) {
	  continue;
	}
	results.put(wrapper);
      }
      
      if (results.length() > 0) {
	return results;
      }
    }

    return results;
  }
}
