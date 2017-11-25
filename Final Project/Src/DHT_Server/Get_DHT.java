import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//for DHT Server 0 only
public class Get_DHT implements Runnable {

	private Socket DHTSocket;
	private InetAddress IP;
	private int DHTClientNum;
	private ArrayList<DHT_Record> DHTList;
	private String threadName;
	private Thread t;
	
	public Get_DHT(String name, Socket client, ArrayList<DHT_Record> records, int serverNum) {
		System.out.println("In Get_DHT constructor(): "+ name +", "+ serverNum);
		this.threadName = name;
		this.DHTSocket = client;
		this.DHTList = records;
		this.IP = DHTSocket.getInetAddress();
		this.DHTClientNum = serverNum;
		updateServerStatus(this.IP, this.DHTList, 1);
		System.out.println("In Get_DHT constructor(): IP is "+ DHTList.get(this.DHTClientNum-1).IPAddr);
	}

	private void updateServerStatus(InetAddress ip2, ArrayList<DHT_Record> dhtList2, int status) {
		//int serverNum = findServerNum(ip2);
		System.out.println("updateServerStatus(): ServerNum is " + this.DHTClientNum);
		System.out.println("updateServerStatus(): ip2 is " + DHTList.get(this.DHTClientNum-1).IPAddr);
		dhtList2.set(this.DHTClientNum-1, new DHT_Record(this.DHTClientNum, DHTList.get(this.DHTClientNum-1).IPAddr, status));
	}
/*
	private int findServerNum(InetAddress ipAddr2) {
		int listSize = DHTList.size(), i;
		DHT_Record nextElem = null;
		
		System.out.println("in findServerNum(): ipAddr2 is " + ipAddr2.getHostAddress());
		System.out.println("in findServerNum(): listSize is " + listSize);
		for (i=1; i<listSize; i++){
			nextElem = DHTList.get(i);
			System.out.println("in findServerNum(): nextElem("+i+")ipAddr2 is " + nextElem.IPAddr);
			if( nextElem.IPAddr.compareTo(ipAddr2.getHostAddress()) == 0 )
				break;
		}
		System.out.println("in findServerNum(): after for loop, i is " + i);
		return DHTList.get(i).serverNum;
	}
*/
	public void start(){
		System.out.println("DHT Server 1 connecting to DHT Client " + this.threadName );
		if ( t == null ){
			t = new Thread(this, this.threadName);
			t.start();
		}
	}
	
	@Override
	public void run() {
/*		int cycles =0;
		while(cycles < 30){
			try {
				Thread.sleep(2000);
				cycles++;
				System.out.println("Sub thread " + threadName + ": sleeping for 2 secs...");
			} catch (InterruptedException e) {
				System.out.println("Sub thread " + threadName + ": closing connections, freeing resources...");
				System.exit(0);
			}
		}
		System.out.println("Sub thread " + threadName + " exited.");
		try {
			DHTSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
*/
		
		String line;
		BufferedReader in = null;
		PrintWriter out = null;
		try{
			DHTSocket.setSoTimeout(3000);	//Wait 2 secs, then check if DHT-DHT connection alive 
			in = new BufferedReader(new InputStreamReader(DHTSocket.getInputStream()));
			out = new PrintWriter(DHTSocket.getOutputStream(), true);
		}
		catch( SocketException e){
			System.out.println("Socket Timeout not set");
			System.exit(-1);
		}
		catch( IOException e){
			System.out.println("in or out failed");
			System.exit(-1);
		}

		while(true){
			try{
				line = "Hello";
		        out.println(line);			//Send data back to client
				System.out.println("DHT Server 1 sends \"Hello\" to DHT Client " + this.DHTClientNum + ", " + this.threadName);
		        TimeUnit.MILLISECONDS.sleep(3000);
		        line = in.readLine();
		        if( line.equals("")){
		        	System.out.println("DHT Client "+ this.IP.getHostAddress() + " connection timed out. DHT Client is offline");
		        	DHTSocket.close();
		        	updateServerStatus(this.IP, this.DHTList, 0);
		        }		        	
		        else if(line.equals("Hi"))
		        	System.out.println("DHT Client " + DHTClientNum + " sends " + line + " to DHT Server 1");
		        else if (line.equals("SHUTDOWN")){
		        	//close all connections and exit thread
		        	DHTSocket.close();
		        	updateServerStatus(this.IP, this.DHTList, 0);
		        	System.exit(-1);
		        }
			}
			catch (IOException e) {
				System.out.println("Read failed");
				updateServerStatus(this.IP, this.DHTList, 0);
				break;
				//System.exit(-1);
			} catch (InterruptedException e) {
				System.out.println("Sleep failed");
				e.printStackTrace();
			}
		}
	}

}
