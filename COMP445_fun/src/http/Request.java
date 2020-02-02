package http;

import java.util.HashMap;

public abstract class Request {
	protected final String SPACE = " ";
	protected final String CRLF = "\r\n";
	protected final String VERSION = "HTTP/1.0";
	private final int PORT = 80;
	protected HashMap<String, String> mHeaders;
	protected String mMethod; // GET or POST
	protected String mURI; // resource url, query parameters are set in this for GET
	private String mHost; // 
	
	/*
	 * allows to pass this object as a string
	 */
	public String toString() {
		return assembleRequest();
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
		mURI = aURI;
	}
	
	public void setHost(String aHost) {
		mHost = aHost;
	}
	
}
