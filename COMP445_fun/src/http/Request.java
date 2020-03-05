package http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.io.IOException;
import java.io.Writer;

public abstract class Request {
	protected final String SPACE = " ";
	protected final String CRLF = "\r\n";
	protected final String VERSION = "HTTP/1.0";
	public static final int PORT = 80;
	protected HashMap<String, String> mHeaders;
	protected String mMethod; // GET or POST
	protected String mPath; // request path
	private String mHost; // 
	
	/*
	 * allows to pass this object as a string
	 */
	public String toString() {
		return assembleRequest();
	}
	
	public void execute(Writer lRequestWriter) {
		try {
			lRequestWriter.write(assembleRequest());
			lRequestWriter.flush();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* 
	 * assemble the request in a string format
	 */
	public abstract String assembleRequest();
	/*
	 *  adds if the header doesn't exist, modifies if it exists
	 */
	public void setHeader(String aHeader, String aValue) {
		mHeaders.put(aHeader, aValue);
	}
	
	public void setURI(String aURI) {
		mPath = aURI;
	}
	
	public void setHost(String aHost) {
		mHost = aHost;
	}
	
	public static String getPathFromUrl(String aURL) {

		URI lUri = null;
		try {
			lUri = new URI(aURL);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    String lPath = lUri.getPath();
	    return lPath;
	}
	
	public static String getHostFromURL(String aURL) {
		URI lUri = null;
		try {
			lUri = new URI(aURL);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    String lDomain = lUri.getHost();
	    return lDomain;
	}
	
	
}
