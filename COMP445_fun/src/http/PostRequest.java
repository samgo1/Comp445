package http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStream;

public class PostRequest extends Request {

	private byte[] mBody;
	private OutputStream mOut;
	
	public PostRequest(OutputStream aOut) {
		mMethod = "POST";
		mHeaders = new HashMap<String, String>();
		mOut = aOut;
		
	}
	
	@Override
	// the top part of the http post request (excluding the body)
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
		
		String lRequest = lRequestLine + lHeaderLines;
								
		return lRequest;
			
	}

	public void execute(Writer aWriter) {
		try {
			aWriter.write(assembleRequest());
			aWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			mOut.write(mBody);
			mOut.flush();
		} catch (IOException e) {
			e.printStackTrace();

		}
		
	}
	public void setBody(byte[] aBody) {
		mBody = aBody;
		
		
	}

}
