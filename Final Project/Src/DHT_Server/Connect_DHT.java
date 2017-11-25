import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Connect_DHT implements Runnable {
	private ServerSocket DHTSSocket;
	private Socket DHTCSocket;
	private ArrayList<DHT_Record> DHTList;
	private int DHTServerNum, DHTClientIdx;	
	//private Thread DHTthreads[];
	//private String DHTServerNames[];
	private Thread t;
	private String threadName;
	
	public Connect_DHT(String name, ArrayList<DHT_Record> records, int serverNum ) {
		this.DHTList = records;
		this.DHTServerNum = serverNum;
		this.threadName = name;
		//DHTthreads = new Thread[3];
		//DHTthreads[0] = DHTthreads[1] = DHTthreads[2] = null;
		//DHTServerNames = new String[3];
		//DHTServerNames[0]="DHT1";
		//DHTServerNames[1]="DHT2";
		//DHTServerNames[2]="DHT3";
		System.out.println("Creating thread " + threadName);
	}
	
	public void start(){
		System.out.println("Starting " + threadName );
		if ( t == null ){
			t = new Thread(this, threadName);
			t.start();
		}
	}

	public void run() {

		if (this.DHTServerNum == 1){	//for DHT Server 0
//			try{	
				try{
					DHTSSocket = new ServerSocket(40410);
				}
				catch( IOException e){
					System.out.println("Could not listen on port 40410");
					System.exit(-1);	
				}
				int serverNum = 2;
				while( serverNum <= 4 ){
					Get_DHT g;
					System.out.println("Connect_DHT: Waiting to connect with server " + serverNum);
					//String serverName = getServerName(DHTList.get(serverNum).IPAddr);
					String serverName = DHTList.get(serverNum-1).IPAddr;
					try{
						g = new Get_DHT(serverName, DHTSSocket.accept(), DHTList, serverNum);
						g.start();
						//DHTthreads[serverNum-1] = new Thread(g);
						//DHTthreads[serverNum-1].start();
						System.out.println("Connect_DHT: connected with server " + serverNum);
						serverNum++;
					}
					catch( IOException e){
						System.out.println("Accept failed: 4444");
						System.exit(-1);
					}
				} //end while
/*			}
			catch( Exception ee){
				System.out.println("Interrupted Exception by User");

					for( int i=0; i<=2; i++){
						if(DHTthreads[i] != null)
							DHTthreads[i].interrupt();
					}
					try {
						DHTSSocket.close();
					} catch (IOException e) {
						System.out.println("Could not close server socket on port 44010");
						e.printStackTrace();
					}

				System.exit(-1);			
			}
*/
		} //end if
		else{							//for DHT Servers 1-3
			Accept_DHT();
		}

	}

	private String getServerName(String ipAddr) {
		String[] token = ipAddr.split("/");
		return token[0];
	}

	//for DHT Servers 1-3
	private void Accept_DHT() {
	
		String line;
		BufferedReader in = null;
		PrintWriter out = null;
		String DHT0Hostname = DHTList.get(0).IPAddr;
		//String DHT0Hostname = getServerName(DHTList.get(0).IPAddr);
		//String DHT0Hostname = "STS-i7-PC";
		
		//DHTClientIdx = get_ClientIdx();
		try {
			DHTCSocket = new Socket(DHT0Hostname, 40410);
		}
		catch( IOException e ){
			System.out.println("DHT Client socket connect failed");
			e.printStackTrace();
			System.exit(-1);
		}

/*		
		int cycles =0;
		while(cycles < 30){
			try {
				//Thread.sleep(2000);
				cycles++;
				System.out.println("Accept_DHT: Sub thread " + "threadName" + ": sleeping for 2 secs...");
			} catch (InterruptedException e) {
				System.out.println("Sub thread " + threadName + ": closing connections, freeing resources...");
				System.exit(0);
			}
		}
		System.out.println("Sub thread " + threadName + " exited.");
*/		
		
	

		try{
			in = new BufferedReader(new InputStreamReader(DHTCSocket.getInputStream()));
			out = new PrintWriter(DHTCSocket.getOutputStream(), true);
		}
		catch( IOException e){
			System.out.println("in or out failed");
			System.exit(-1);
		}
		while(true){
			//DHT_Record rec = DHTList.get(DHTClientIdx);
			try{
				line = in.readLine();
				System.out.println("DHT Client " + DHTList.get(DHTServerNum-1).IPAddr + ": DHT Server says " + line);
		        out.println("Hi");			//Send data back to client
		        //TimeUnit.MILLISECONDS.sleep(1000);
			}
			catch (IOException e) {
				System.out.println("Read failed");
				System.exit(-1);
			}
			catch (Exception f) {
				System.out.println("Interrupted Exception by User");
				out.println("SHUTDOWN");
				try {
					DHTCSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.exit(-1);
			}

			finally{}
		}
	}

	
//	------------ NOT USED ---------------------------------------------------------------------------------------
/*
	//gets DHT Client's index in DHTList based on its IP address
	private int get_ClientIdx() {
		InetAddress localHost;
		int clientIdx = -1;
		try{
			localHost = InetAddress.getLocalHost();
			clientIdx = findServerNum(localHost);
		}
		catch (IOException e) {
			System.out.println("Cannot get IP of local host.");
			System.exit(-1);
		}
		return clientIdx;
	}

	private int findServerNum(InetAddress ipAddr2) {
		int listSize = DHTList.size(), i;
		DHT_Record nextElem = null;
		for (i=1; i<listSize; i++){
			nextElem = DHTList.get(i);
			if( nextElem.IPAddr.compareTo(ipAddr2.toString()) == 0 )
				break;
		}
		return i;
	}
*/	
}

