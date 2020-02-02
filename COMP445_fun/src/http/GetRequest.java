package http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class GetRequest extends Request {

	public GetRequest() {
		mMethod = "GET";
		mHeaders = new HashMap<String, String>();
	}
	@Override
	public String assembleRequest() {
		String lRequestLine = mMethod + SPACE + mURI + SPACE + VERSION + CRLF;
		// building my header line(s)
		String lHeaderLines = "";
		Iterator lIt = mHeaders.entrySet().iterator();
		while (lIt.hasNext()) {
			Map.Entry<String, String> lMapEntry = (Map.Entry)lIt.next();
			String lHeader = lMapEntry.getKey();
			String lValue = lMapEntry.getValue();
			lHeaderLines += lHeader + ":" + lValue + CRLF;
			
		}
		
		lHeaderLines += CRLF;
								
		String lRequest = lRequestLine + lHeaderLines;
		return lRequest;
			
	}	

}
