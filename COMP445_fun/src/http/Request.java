package http;

import java.util.HashMap;

public abstract class Request {
	private final int mPort = 80;
	private final String mVersion = "HTTP/1.0";
	private HashMap<String, String> mHeaders;
	private String mVerb; // GET or POST
	private String mHost;
	
	public abstract void assembleRequest();
	/*
	 *  adds if the header doesn't exist, modifies if it exists
	 */
	public void setHeader(String aHeader, String aValue) {
		mHeaders.put(aHeader, aValue);
	}
	
	public void setHost(String aHost) {
		mHost = aHost;
	}
	
	
	
	
	
	
}
