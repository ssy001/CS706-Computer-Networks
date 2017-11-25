mport java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;


public class P2P_Client {

	public static ArrayList<Content_Record> recordList;
    static ArrayList<DHT_Record> DHTServerList;
	public static Content_Record requestedRecord;
	
	public static final int drcvport = 40412;	//UDP port number of this client when connecting with DHT servers
	public static final int dsndport = 40410;	//UDP port number of DHT server when connecting with DHT servers
	public static final int tcpsndport = 40411;	//UDP port number of DHT server when connecting with DHT servers
	public static String DHT0IP = "ENG201-15";
	static File dir = new File("./sharedDir");
	final static String sharedDir = "./sharedDir";
	static File[] content;
	
	public static void init_Vars(){
	    /*	Create/instantiate new object DHTserverList[4]. 
			Create/instantiate new object recordList. 
			Create/instantiate variable requestedRecord. */ 
		  DHTServerList = new ArrayList<DHT_Record>(); 
		  recordList = new ArrayList<Content_Record>();
		  requestedRecord = new Content_Record();
    }
	
	public static void init(){ 
		/*	1.	Store the 1st DHT server (DHTserverNum, IPaddr) record.
			2.	Create UDP socket
			3.	Do
					Request (UDP:GETDHT) DHTserverList[0] for IP addresses of remaining servers.
				While return message code is not equal to SENDDHT
				Retrieve DHT servers 1-3 info from message
				Close socket.
		*/ 
		
		DatagramSocket sndsock = null;
		try{
			sndsock = new DatagramSocket(drcvport);
			InetAddress host = InetAddress.getByName(DHT0IP);
			System.out.println("host: " + host.getHostAddress().toString());
			String request = "GETDHT"; 

			byte[] b = request.getBytes();
			DatagramPacket  dp = new DatagramPacket(b , b.length , host , dsndport);
			//Create a buffer to read datagrams into. 
			byte[] buffer = new byte[2048];
			// Create a packet to receive data into the buffer
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			
			String return_msg = "";
			
			do {	//loop, waiting to receive packets i.e. until receive SENDDHT message code.
				sndsock.send(dp);
				sndsock.receive(reply);
				return_msg = new String(reply.getData(), 0, reply.getLength());
				System.out.println(return_msg);
			} while(!return_msg.contains("SENDDHT")); 
			System.out.println("-----here1-----");
			
			// Store DHT Server 0 info in first 0th entry of DHTServerList 
			DHTServerList.add(new DHT_Record(0, InetAddress.getByName(DHT0IP).getHostAddress(), 1));
			// Retrieve and store DHT Servers 1-3 info from message
			StringTokenizer st1 = new StringTokenizer(return_msg, "\n");
			String msg = st1.nextToken();
			while(st1.hasMoreTokens()) {
				String servers = st1.nextToken(); 
				StringTokenizer st2 = new StringTokenizer(servers, ",");
				while(st2.hasMoreTokens()) {
					String num = st2.nextToken(); 
					int n = Integer.parseInt(num);
					String ip = st2.nextToken();
					String status = st2.nextToken();
					int stat = Integer.parseInt(status);
					DHTServerList.add(new DHT_Record(n, ip, stat));
				}
				System.out.println("-----here1-----");
			}
			sndsock.close();
		} catch (SocketException se) {
			System.out.println("ERROR: Cannot create socket. "+se);
			System.exit(-1);
		} catch (IOException ioe) {
			System.out.println("ERROR: Cannot send or receive from socket. "+ioe);
			sndsock.close();
		}
	} 
	
	public static void query_For_Content(String fileName){
		String contentName = fileName;
		int DHTserverNum = hash(contentName);
		System.out.println("hashed DHTserverNum is: " + DHTserverNum);
		
		DHT_Record record = DHTServerList.get(DHTserverNum);
		DatagramSocket sndsock = null;
		DatagramPacket reply = null;
		InetAddress ip_addr = null;
		try {
			ip_addr = InetAddress.getByName(record.get_IPAddr());
			sndsock = new DatagramSocket(drcvport);
			
			/* Send (UDP:REQINFO) record(contentName) to DHTserverNum */
			String request_msg = "REQINFO\n"+ contentName;	
			byte[] b = request_msg.getBytes();
			DatagramPacket  sendPacket = new DatagramPacket(b , b.length , ip_addr, dsndport); 
			sndsock.send(sendPacket);
			
			/*Receive UDP response message from DHT server*/
			byte[] buffer = new byte[2048];		// Create a packet to receive data into the buffer
			reply = new DatagramPacket(buffer, buffer.length);
			sndsock.receive(reply);				// Receive UDP response message from DHT server
			String return_msg = new String(reply.getData(), 0, reply.getLength());
			sndsock.close();					// Close UDP socket
			
			String P2PServerIP = "";
			if (return_msg.contains("CODE404")) {
				System.out.println("Content "+ contentName +" not found");
				sndsock.close();
				System.exit(-1);
			}
			else if(return_msg.contains("INFORM")) {	// Determine IP address of P2P Server				   
				StringTokenizer st1 = new StringTokenizer(return_msg, "\n");
				String msg = st1.nextToken();
				while(st1.hasMoreTokens()) {
					String servers = st1.nextToken(); 
					StringTokenizer st2 = new StringTokenizer(servers, ",");
					while(st2.hasMoreTokens()) {
						contentName = st2.nextToken(); 
						P2PServerIP = st2.nextToken();
					}
					break;	//retrieves the first IP address in the INFORM msg list.
				}
			}
			
			Get_Content(P2PServerIP, contentName);  
		} catch (UnknownHostException uhe) {
			System.out.println("ERROR: Unknown host. "+uhe);
			sndsock.close();
			System.exit(-1);
		} catch (IOException ioe) {
			System.out.println("ERROR: Socket send or receive error. "+ioe);
			System.exit(-1);
		}
	}// end of query_For_Content
	
	public static int hash(String content){
		/*
		sum decimal values of ASCII characters of content name
		set x to sum of decimal values
		set y to x mod 4
		set DHT ID to Y + 1 (actually, just Y for our implementation)
		*/
		int x=0, y=0;
		for(int i = 0; i < content.length(); i++) {
			char character = content.charAt(i);
			int ascii = (int) character;
			x = x + ascii;
		}
		y = x % 4;	//y's range is 0..3 - this indexes into DHTServerList (ArrayList)
		return y;
	}//End of hash
	
	public static void Get_Content(String P2PServerIP,  String contentName) 
	{
	/*		
		download content file (via TCP) from remote P2P server*
		Get_Content(P2P Server IP, content){
			Open TCP socket (P2P server IPaddr)
			Set Request Message code to GET
			Set HTTP version to 1.1
			Send Request (content) Message
			Receive Response Message
			While true
				if Response Code = 200
					Save to file
					Output message: File downloaded successfully. 
					Break
				Else If Response Code = 400
					Set message code to GET 
					Send request (content)
				Else if Response Code = 404
					Output message: file not found.
					Break.
				Else if Response Code = 505
					Output message: HTTP version not supported
					Break.
			End while
			Close TCP socket
		}
	*/
		Socket P2PSocket = null;
        PrintWriter output = null;
        BufferedReader in = null;
		
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		int bytesRead;

		//BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		try {
			P2PSocket = new Socket(P2PServerIP, tcpsndport); 
            output = new PrintWriter(P2PSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(P2PSocket.getInputStream()));
			//InputStream is = socket.getInputStream();
			
			//ois = new ObjectInputStream(new BufferedReader(new InputStreamReader(socket.getInputStream())));
			//BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        } catch (UnknownHostException e) {
            System.err.println("Unknown host");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not get I/O for the connection");
            System.exit(1);
        }
		String request = "HTTP1.1\n"+"GET\n" +  contentName; 
		
		output.println(request);
		//output.flush();
		System.out.println(request);
		/* Receive Response Message */
		String response = "";
		try {
			//response = (String) ois.readObject();
			response = in.readLine();   //NEED TO CHANGE (?)
			System.out.println("Response received from the server: " + response);
			response = in.readLine();   //NEED TO CHANGE (?)
			System.out.println("Response received from the server: " + response);
		}
		catch(Exception e) {
			System.out.println("Problem reading back from server: " + e);
		}

		// Interpreting HTTP response code
		if (response.contains("200")) {
			System.out.println("Response Message: " + response);
			StringTokenizer st = new StringTokenizer(response, "\n");
			String httpver = st.nextToken();
			String msgcode = st.nextToken();
			String filename = st.nextToken();
			String filesize = st.nextToken();
			String filedataUTF = st.nextToken();
			convertFromUTF(filename, filedataUTF);
		}
		else if(response.contains("400")) {
			/*	Set message code to GET 
				Send request (content)
			*/
			output.println(request);
			output.flush();
		}
		else if(response.contains("404")) {
			// Output message: file not found. Break.
			System.out.println("file not found.");
			break;
		}
		else if(response.contains("505")) {
			// Output message: HTTP version not supported. Break.
			System.out.println("HTTP version not supported");
			break;
		}
			
		try {
			//close connection
			in.close();
			output.close();			
		} catch (IOException ioe) {
			System.out.println("ERROR: "+ioe);
		}
	} //END of Get_Content
	
	public static void convertFromUTF(String fname, String fdataUTF){
		byte[] fdataBytes = fdataUTF.getBytes("UTF-8");
		File outfile = new File(dir, fname);
		FileOutputStream fos = null;
		fos = new FileOutputStream(outfile);
		fos.write(fdataBytes);
		fos.flush();
		fos.close();
	
	}

	public static void main (String [] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter computer name of DHT Server 1: ");
		DHT0IP = scanner.nextLine();

		init_Vars();
		init();
		
		System.out.println("Please enter name of the file to download: ");
		String fn = scanner.nextLine();
		String name = "";
		int flag = 0;
        try { 	
			File[] retrieve =  finder(sharedDir, fn);
			for (int i = 0; i < retrieve.length; i++) { 
				name = retrieve[i].getName();
				System.out.println("Filename: " + name);
				if(!fn.equals(name)) {
					System.out.println("File "+ fn + "is already downloaded.");
					flag = 1;
					break;
				}
			}
			if (flag == 0) {
				query_For_Content(fn);
			}
        } catch (Exception e) {
        	System.out.println("File operation error.");
            e.printStackTrace();
            System.exit(-1);
        }
	}
	
	public static File[] finder( String dirName, String fn) {
		final String fileName = fn;
		File dir = new File(dirName);

		return dir.listFiles(new FilenameFilter() { 
			public boolean accept(File dir, String filename) {
				return filename.equals(fileName); 
			}
		});
	}
	
}


