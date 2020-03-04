package http;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Response {
	private InputStream mInputStream;
	private String mVersion;
	private int mStatusCode;
	private String mPhrase;
	private String mBody;
	private HashMap<String, String> mHeaders;
	private final String END_OF_HEADERS = "";
	
	public Response(InputStream aInputStream) {
		mInputStream = aInputStream;
		mHeaders = new HashMap<>();
		mBody = "";
		// parsing the returned response
		parse();
	
	}
	
	private void parse() {
		BufferedReader lBuffReader = new BufferedReader(new InputStreamReader(mInputStream));
		String lLine;
		int lCounter = 0;
		try {
			boolean lNeedToProcessHeaders = true;
			boolean lNeedToProcessBody = false;
			while ((lLine = lBuffReader.readLine()) != null) {
				if (lCounter == 0) {
					// lLine is status line
					mVersion = lLine.substring(0, lLine.indexOf(' '));
					mStatusCode = Integer.parseInt
							(lLine.substring(lLine.indexOf(' ')+1,lLine.indexOf(' ') + 4));
					mPhrase = lLine.substring(lLine.indexOf(Integer.toString(mStatusCode)) + 3);
					lCounter++;
					continue;
					}
				
				if (!lLine.equals(END_OF_HEADERS) && lNeedToProcessHeaders) {
					// grab the headers
					String lHeaderKey = lLine.substring(0, lLine.indexOf(':'));
					String lHeaderVal = lLine.substring(lLine.indexOf(':')+1);
					mHeaders.put(lHeaderKey, lHeaderVal);
					lCounter++;
					continue;
					
				}
				
				if (lLine.equals(END_OF_HEADERS) && lNeedToProcessBody == false) {
					lCounter++;
					lNeedToProcessHeaders = false;
					lNeedToProcessBody = true;
					continue; // next one is body
				}
				
				
				mBody += lLine + "\n";
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void print(boolean aVerbose) {
		if (aVerbose) {
			// print all of the request
			System.out.println(mVersion + " " + mStatusCode + " " + mPhrase);
			// printing the headers
			Iterator lIt = mHeaders.entrySet().iterator();
			while (lIt.hasNext()) {
				Map.Entry<String, String> lMapEntry = (Map.Entry)lIt.next();
				String lHeader = lMapEntry.getKey();
				String lValue = lMapEntry.getValue();
				System.out.println(lHeader + ":" + lValue);
			}			
			System.out.println();
		}
		
		System.out.print(mBody);
	}
	
	
}
