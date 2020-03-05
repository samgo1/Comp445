package http.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Server {
	
	private ServerSocket mServerSocket;
	private String mDirectory;
	
	private final String STATUS_LINE_200 = "HTTP/1.0 200 OK \r\n";
	
	public Server(int aPort, String aDirectory) {
		try {
			mServerSocket = new ServerSocket(aPort);			
		} catch(IOException e) {
			System.out.print(e.getMessage());
			e.printStackTrace();
		}
		mDirectory = aDirectory;
	}
	
	public void run() {
		while (true) {
			try {
				Socket lSocket = mServerSocket.accept();
				InputStream lInputstream = lSocket.getInputStream();
				OutputStream lOutputStream = lSocket.getOutputStream();
			    Writer lWriter = new BufferedWriter(new OutputStreamWriter(lOutputStream));
				// determine what is requested from the user
				BufferedReader lBuffReader = new BufferedReader(new InputStreamReader(lInputstream));
				String lRequestLine = lBuffReader.readLine();
				String lResourcePath = getPathfromRequestLine(lRequestLine);
				if (lRequestLine.contains("GET")) {
					if (lResourcePath.contentEquals("/")) {
						// write all the files in the response to be sent
						writeGetFilesResponse(lWriter);
					} else {
						writeFileResponse(lWriter, lResourcePath);
					}
				} else if (lRequestLine.contains("POST")) {
					writewriteFileResponse(lWriter, lResourcePath);
				}
				
				lSocket.close();
			} catch(IOException e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void writeGetFilesResponse(Writer aWriter) {
		String lBody = "";
		try {
			aWriter.write(STATUS_LINE_200);
			// add headers here, each line ended with \r\n
			aWriter.write("\r\n"); // end of headers 
			/* making the body */
			File lFolder = new File(mDirectory);
			File[] lListOfFiles = lFolder.listFiles();

			for (File lFile : lListOfFiles) {
			    if (lFile.isFile()) {
			        lBody += lFile.getName() + "\r\n";
			    }
			}
			aWriter.write(lBody); // body
			aWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void writeFileResponse(Writer aWriter, String aFile) {
		
	}
	
	private void writewriteFileResponse(Writer aWriter, String aFile) {
		
	}
	
	private String getPathfromRequestLine(String aRequestLine) {
		int lStartingIndex = aRequestLine.indexOf(' ');
		int lEndIndex = aRequestLine.indexOf(' ', lStartingIndex + 1);
		String lPath = aRequestLine.substring(lStartingIndex+1, lEndIndex);
		return lPath;
		
	}
	
	
}
