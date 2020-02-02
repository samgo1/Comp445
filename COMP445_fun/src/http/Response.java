package http;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Response {
	private InputStream mInputStream;
	private String mVersion;
	private int mStatusCode;
	private String mPhrase;
	private HashMap<String, String> mHeaders;
	
	public Response(InputStream aInputStream) {
		mInputStream = aInputStream;
		mHeaders = new HashMap<>();
	
	}
	
	private void parse() {
		BufferedReader lBuffReader = new BufferedReader(new InputStreamReader(mInputStream));
		String lLine;
		int lCounter = 0;
		try {
			while ((lLine = lBuffReader.readLine()) != null) {
				if (lCounter == 0) {
					// lLine is status line
					mVersion = lLine.substring(0, lLine.indexOf(' '));
					mStatusCode = Integer.parseInt
							(lLine.substring(lLine.indexOf(' '),lLine.indexOf(' ') + 3));
					mPhrase = lLine.substring(lLine.indexOf(Integer.toString(mStatusCode)) + 3
							, lLine.indexOf("/r"));				
					}
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
