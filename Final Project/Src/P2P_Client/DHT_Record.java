
public class DHT_Record {

	public int serverNum;
	public String IPAddr;
	public int status;  

	
	public DHT_Record() {
		serverNum = -1;
		IPAddr = "";
		status = 0;
	}
	
	public DHT_Record(int sNum, String IP, int st) {
		serverNum = sNum;
		IPAddr = IP;
		status = st;
	}

	public String get_IPAddr(){ return this.IPAddr; }
	public void set_IPAddr(String ip){ this.IPAddr = ip; }

	public int get_serverNum(){ return this.serverNum; }
	public void set_serverNum(int num){ this.serverNum = num; }

	public void set_status(int s){ this.status = s; }
	

}
