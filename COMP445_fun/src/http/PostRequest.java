package http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PostRequest extends Request {

	private String mBody;
	
	public PostRequest() {
		mMethod = "POST";
		mHeaders = new HashMap<String, String>();
		mBody = "";
	}
	
	@Override
	public String assembleRequest() {
		String lRequestLine = mMethod + SPACE + mPath + SPACE + VERSION + CRLF;
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
		
		String lRequest = lRequestLine + lHeaderLines + mBody;
								
		return lRequest;
			
	}
	
	public void setBody(String aBody) {
		mBody = aBody;
	}

}
