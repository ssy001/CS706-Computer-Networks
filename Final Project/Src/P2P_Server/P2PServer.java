
import java.lang.*;
import java.net.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;


public class P2PServer {
	// global variables
    static ServerSocket server;
    static ArrayList<Record> recordList;
    static ArrayList<DHTList> DHTServerList;
    static Record requestedRecord;
    static int srvSockPort = 40411; // groups port number;
	static File dir;
	static File[] Gcontent;
	
	/*	MAIN
	 *	1. initialize global variables
	 *	2. initialize DHT Servers and get information
	 *	3. go to shared directory and for each file in directory inform and update dht servers
	 *	4. create server socket. if accepted create and start client thread which processes client requests
	 * 	5. if user sends quit signal (^C) then quit program
	 * 	6. cat content again for recently added content in directory
	 */	
    public static void main(String[] args){
		init_Vars();
		init();
		cat_Content();
        try{
            //creates and binds tcp socket to port servSock
            server = new ServerSocket(srvSockPort);
            while ( true ){
				Client_Thread clientThread = new Client_Thread(server.accept(), dir, Gcontent);
				clientThread.start();
				cat_Content();
            }
        } catch (Exception e){
			System.out.println("Thread error: "+e);
            try { server.close(); } catch (IOException ioe) { System.out.println("ioexception: "+ ioe); }
        } //End try-catch
    }//End main
	
	/*	init_Vars
	 *	1. instantiate new DHTList arraylist
	 *	2. instantiate new Record arraylist
	 *	3. instantiate new Record
	 */
	public static void init_Vars(){
        DHTServerList = new ArrayList<DHTList>();
        recordList = new ArrayList<Record>();
        requestedRecord = new Record();
	}

	/*	init
	 *	1. create new udp packet
	 *	2. ask user for main dht server name
	 *	3. get ipaddress of main dht server
	 *	4. add dht server to DHT arraylist
	 *	5. send "GETDHT" message to main dht
	 *	6. receive "SENDDHT" message from main dht
	 *	7. parse message to get ids of other dht servers
	 *	8. also parse message to get ip and status(online/offline) of other dht servers
	 *	9. once all information received, add dht servers to DHT arraylist
	 *	10. close socket
	 */
    public static void init(){
		String msg; String servernum="0",servername=""; Scanner input = new Scanner(System.in);
		DHTList dht0;
		try {
			byte[] buffer = new byte[1024];
			DatagramPacket udprcvPack = new DatagramPacket(buffer, buffer.length);
			DatagramSocket udpSock = new DatagramSocket(40411);
			System.out.print("Please enter DHT Server computer name (ex. ENG201-15): ");
			servername = input.nextLine();
			InetAddress ipaddr = InetAddress.getByName(servername);
			dht0 = new DHTList(servernum, ipaddr.getHostAddress().toString(), "1");
			DHTServerList.add(dht0);
			do {
				msg = "GETDHT";
				byte[] sendmsg = msg.getBytes("UTF-8");
				DatagramPacket udpsndPack = new DatagramPacket(sendmsg, sendmsg.length, ipaddr, 40410);
				udpSock.send(udpsndPack);
				udpSock.receive(udprcvPack);
				msg = new String( udprcvPack.getData(), 0, udprcvPack.getLength() );
				System.out.println("receivemsg = " + msg);
			} while ( !msg.contains("SENDDHT") );
			String delim1 = "\n", delim2 = ",";
			String[] tokens, dhtserv1, dhtserv2, dhtserv3;
			tokens = msg.split(delim1);
			dhtserv1 = tokens[1].split(delim2);
			dhtserv2 = tokens[2].split(delim2);
			dhtserv3 = tokens[3].split(delim2);
			DHTList dht1 = new DHTList(dhtserv1[0], dhtserv1[1], dhtserv1[2]);
			DHTList dht2 = new DHTList(dhtserv2[0], dhtserv2[1], dhtserv2[2]);
			DHTList dht3 = new DHTList(dhtserv3[0], dhtserv3[1], dhtserv3[2]);
			DHTServerList.add(dht1);
			DHTServerList.add(dht2);
			DHTServerList.add(dht3);
			udpSock.close();
		} catch (SocketException se) {
			System.out.println("ERROR: " + se);
		} catch (IOException ioe) {
			System.out.println("ERROR: " + ioe);
		}
    }

	/*	cat_Content
	 *	1. initialize directory to the shared directory
	 *	2. initialize list of files into variable
	 *	3. go through list of files
	 *	3.1 if content is a file
	 *	3.1.1 inform and update dht servers of file
	 */
	public static void cat_Content(){
		dir = new File("./sharedDir"); System.out.println("directory = " + dir.getAbsolutePath());
		Gcontent = dir.listFiles();
		for (int i = 0; i < Gcontent.length; i++){
			if (Gcontent[i].isFile()) {
				System.out.println("File name: " + Gcontent[i].getName());
				inform_And_Update(Gcontent[i].getName());
			}
		}
    }

	/*	inform_And_Update
	 *	1. get and initialize filename
	 *	2. initialize dht server to hash of filename
	 *	3. if dht server is online
	 *	3.1 send an "INFORM" messgae with filename to dht server
	 *	4. keep a personal record in Record
	 */
	public static void inform_And_Update(String contentName){
		String filename = contentName, message = "", dhtnum="", ipaddress="0.0.0.0";
		DatagramSocket udpsndSock = null;
		int dhtid = hash(filename); System.out.println("dhtid " + dhtid);
		DHTList dht = DHTServerList.get(dhtid);
		if (dht.getStatus().contains("1")){
			try {
				udpsndSock = new DatagramSocket(40411);
				DatagramPacket udprcvPack = new DatagramPacket(new byte[4096] , 4096);
				InetAddress ipaddr = InetAddress.getByName(dht.getDHTIP());
				System.out.println("ipaddr = " + ipaddr.toString());
				String msg = "INFORM\n"+filename; dhtnum = dht.getDHTNum(); ipaddress = dht.getDHTIP();
				byte[] sendmsg = msg.getBytes("UTF-8");
				DatagramPacket udpsndPack = new DatagramPacket(sendmsg, sendmsg.length, ipaddr, 40410);
				udpsndSock.send(udpsndPack);
				udpsndSock.close();
			} catch (UnknownHostException uhe){
				System.out.println("ERROR: "+uhe);
			} catch (SocketException se){
				System.out.println("ERROR: "+se);
			} catch (UnsupportedEncodingException uee) {
				System.out.println("ERROR: "+uee);
			} catch (IOException ioe) {
				System.out.println("ERROR: "+ioe);
				udpsndSock.close();
			}
		}
		Record record = new Record(filename, dhtnum, ipaddress);
		recordList.add(record);
	}

	/* hash
	 * 1. get filename
	 * 2. for each character in filename
	 * 2.1 convert character to int
	 * 2.2 set x to int character
	 * 3. set y to x mod 4
	 * 4. return result
	 */
	public static int hash(String content){
		String cntnt = content;
		int x=0,y;
		for (int i=0; i < content.length(); i++){
			char character = cntnt.charAt(i);
			int ascii = (int) character;
			x = x + ascii;
		}
		y = x % 4;
		return y;
	}
}

/* class runnable
 * Client_Thread
 */
class Client_Thread implements Runnable{
	private Thread t;
	int i;
	
	/* start
	 * 1. create new thread
	 * 2. start thread
	 */
	public void start(){
		i = 0;
		System.out.println("Starting client thread "+ i++ );
		if (t == null){
			t = new Thread(this);
			t.start();
		}
	}
	
	Socket clientSock;
	String content="", delim="\n", msg="", filename="";
	String[] tokens, files;
	/* constructor: Client_Thread
	 * 1. get client socket
	 * 2. get directory
	 * 3. get contents of files
	 * 4. go through directory
	 * 4.1 if content is file
	 * 4.2 get file name
	 */
	public Client_Thread(Socket srvSock, File file, File[] contnt){
		clientSock = srvSock;
		File directory = file;
		File[] contents = contnt;
		files = new String[contents.length];
		for (int i = 0; i < directory.listFiles().length; i++){
			if (contents[i].isFile()){
				files[i] = contents[i].getName();
				System.out.println("file = " + files[i]);
			}
		}
	}
	
	/* run
	 * 1. main method which runs the thread
	 * 2. create reader and writer
	 * 3. while true
	 * 3.1 read packet and while there is something in packet
	 * 3.1.1 check if string contains .jpg
	 * 3.1.1.1 if true break
	 * 3.2 if content in packet contains "HTTP1.1"
	 * 3.2.1 if content contains "GET"
	 * 3.2.2 get filename
	 * 3.2.3 find filename in directory
	 * 3.2.4 send file to client
	 * 3.2.5 create file.jpg into unicode string
	 * 3.2.6 create message with http message
	 * 3.2.7 send message
	 * 3.2.8 break
	 * 3.3.1 if file not found send message http 404
	 * 3.4 close writer reader and socket
	 * 3.5 if message isn't "HTTP1.1"
	 * 3.5.1 send message 505
	 * 3.6 if message isn't "HTTP"
	 * 3.6.1 send message not found
	 */
	public void run(){
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			PrintWriter pw = new PrintWriter(clientSock.getOutputStream(), true);
			while (true){
			String str = "";
				while ((str=br.readLine()) != null){
					content += str + "\n";
					if (str.contains(".jpg")){ break; }
				}
System.out.println("Received message is: " + content);
				if ( content.contains("HTTP1.1") ){
					if ( content.contains("GET") ){
						tokens=content.split(delim);
						filename = tokens[2];
						for (int i = 0; i < files.length; i++){
							if (filename.equals(files[i])){
								File file = new File("./sharedDir/"+filename);
								long filesize = file.length();
								msg = "HTTP1.1\n200\n" + filename + "\n" + filesize + "\n";
								/*
								byte[] bytefile = new byte[(int) file.length()];
								BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
								bis.read(bytefile, 0, bytefile.length);
								OutputStream os = clientSock.getOutputStream();
								os.write(bytefile, 0, bytefile.length);
								os.flush();
								*/
								String unicodefile = convertToUTF(file);
								msg += unicodefile;
                                pw.println(msg);
								//pw.flush();
								break;
							} else if (i == files.length-1) {
								msg = "HTTP1.1\n404";
								pw.println(msg);
								break;
							}
						}
						pw.close();
						br.close();
						clientSock.close();
						break;
					}
				} else if ( content.contains("HTTP") ){
					msg = "HTTP:\n505";
					pw.println(msg);
					pw.close();
					br.close();
					clientSock.close();
					break;
				}
				else {
					msg = "400\n";
					pw.println(msg);
					pw.close();
					br.close();
					clientSock.close();
					break;
				}
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
	
	/* convertToUTF
	 * 1. get file to convert
	 * 2. create reader and writer
	 * 3. for each character in file.jpg
	 * 3.1 write character to buffer
	 * 4. convert buffer to byte array
	 * 5. set string variable to "UTF-8" string
	 * 6. return unicode string
	 */
	public static String convertToUTF(File file){
		try {
			/*
			 * 1. How to convert an image file to  byte array?
			 */
			File fl = file;
			FileInputStream fis = new FileInputStream(fl);
			//ByteArrayInputStream bis = new ByteArrayInputStream(new(BufferedInputStream(fis)));
			byte[] bytes = new byte[(int) fl.length()];
			fis.read(bytes);
			String unicodeString = new String(bytes, "UTF-8");
			return unicodeString;
		} catch (FileNotFoundException fnfe) {
			System.out.println("ERROR: " + fnfe);
			return null;
		} catch (IOException ioe) {
			System.out.println("ERROR: " + ioe);
			return null;
		}
	}
}

/* Object
 * Record
 */
class Record{
	String content, DHTserver, ipAddr;
	
	/* constructor: Record
	 * 1. set content to empty string
	 * 2. set ipaddress to empty string
	 */
	public Record(){
		content="";
		ipAddr="";
	}
	
	/* constructor: Record
	 * 1. set content to filename
	 * 2. set dht server to dht server id
	 * 3. set ipaddress to ipaddress of dht server
	 */
	public Record(String cont, String DHTnum, String ipa){
		content = cont;
		DHTserver = DHTnum;
		ipAddr = ipa;
	} 
}

/* class
 * DHTList
 */
class DHTList{
	String serverNum, ipAddr, status;
	
	/* constructor: DHTList
	 * 1. set server number to id of dht server
	 */
	public DHTList(String srvnum){
		serverNum = srvnum;
	}
	
	/* constructor: DHTList
	 * 1. set server number to id of dht server
	 * 2. set ipaddress to ipaddress of dht server
	 */
	public DHTList(String srvnum, String ipaddr){
		serverNum = srvnum;
		ipAddr = ipaddr;
	}
	
	/* constructor: DHTList
	 * 1. set server number to id of dht server
	 * 2. set ipaddress to ipaddress of dht server
	 * 3. set status to status of dht server
	 */
	public DHTList(String srvnum, String ipaddr, String stat){
		serverNum = srvnum;
		ipAddr = ipaddr;
		status = stat;
	}
	
	/* getDHTIP
	 * 1. return dht server ipaddress
	 */
	public String getDHTIP(){
		return ipAddr;
	}
	
	/* setDHTIP
	 * 1. set ipaddress of dht server 
	 */
	public void setDHTIP(String ip){
		ipAddr = ip;
	}
	
	
	/* getDHTNum
	 * 1. return dht server id
	 */
	 public String getDHTNum(){
		return serverNum;
	}
	
	/* getStatus
	 * 1. return dht status
	 */
	public String getStatus(){
		return status;
	}
}

