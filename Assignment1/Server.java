import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
	// a unique ID for each connection
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// // if I am in a GUI
	// private ServerGUI sg;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// block duration
	private int block_duration;
	// timeout
	private int timeout;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;
	// the hashmap stored login information
	private Map<String, String> loginmap;
	

	/*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
	 */
	public Server(int port, int block_duration, int timeout) {
		this.port = port;
		this.block_duration = block_duration;
		this.timeout = timeout;
		sdf = new SimpleDateFormat("HH:mm:ss");
		al = new ArrayList<ClientThread>();
	}
	
	// public Server(int port, ServerGUI sg) {
	// 	// GUI or not
	// 	this.sg = sg;
	// 	// the port
	// 	this.port = port;
	// 	// to display hh:mm:ss
	// 	sdf = new SimpleDateFormat("HH:mm:ss");
	// 	// ArrayList for the Client list
	// 	al = new ArrayList<ClientThread>();
	// }
	
	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while(keepGoing) 
			{
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");
				
				Socket socket = serverSocket.accept();  	// accept connection
				// if I was asked to stop
				if(!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);  // make a thread of it
				al.add(t);									// save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
						// not much I can do
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}		
    /*
     * For the GUI to stop the server
     */
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement 
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
			// nothing I can really do
		}
	}

	/*
	 * Display an event (not a message) to the console or the GUI
	 */
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		// if(sg == null)
		System.out.println(time);
		// else
		// 	sg.appendEvent(time + "\n");
	}
	/*
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		//String messageLf = time + " " + message + "\n";
		String messageLf = message + "\n";
		// display message on console or GUI
		// if(sg == null)
		System.out.print(messageLf);

		
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	
	/*
	 *  To run as a console application just open a console window and: 
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	public static void main(String[] args) {
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		int block_duration_time = 10000;
		int timeout_time = 10000;
		switch(args.length) {
			case 3:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber][block_duration][timeout]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber][block_duration][timeout]");
				return;		
		}

		// create a server object and start it
		Server server = new Server(portNumber,block_duration_time,timeout_time);
		server.start();
	}

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// my unique id (easier for deconnection)
		int id;
		// the Username of the Client
		String username;
		// the login password
		String password;
		// the only type of message a will receive
		ChatMessage cm;
		// the date I connect
		String date;
		// the timestamp I connect
		long loginTimeStamp;
		// hashmap stroed login infor
		Map<String, String> loginmap;

		// Constructore
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			System.out.println("This user id is: " + id);
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				password = (String) sInput.readObject();

			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
            loginTimeStamp = new Date().getTime();

		}

		// what will run forever
		public void run() {

            boolean authentification = checkLogin(username, password);

			while(authentification) {
				// read a String (which is an object) from client input
				try {
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}

				// the messaage part of the ChatMessage
				String message = cm.getMessage();

				// Switch on the type of message receive
				switch(cm.getType()) {
				case ChatMessage.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					broadcast(username + " logged out");
					authentification = false;
					break;
				case ChatMessage.WHOELSESINCE:
					System.out.println("The WHOELSESINCE argument is: " + message);
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						String loginTime = ct.date;

						long connectTime = ct.loginTimeStamp;
						long queryTime = new Date().getTime();
						if ((connectTime - queryTime)/1000 < Integer.parseInt(message)){
							if (username.equals(ct.username)){
								continue;
							} else{
								writeMsg(ct.username);
							}
						} else{
							continue;
						}
					}
					break;
				case ChatMessage.WHOELSE:
					// writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan al the users connected
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						if (username.equals(ct.username)){
							continue;
						} else {
							writeMsg(ct.username);
						}
					}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		// check login info
		private boolean checkLogin(String msg1, String msg2) {
			Map<String, String> loginmap = new HashMap<String, String>();
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/Users/rongchenxuan/Desktop/credentials.txt")),"UTF-8"));

				String lineTxt = null;
				while ((lineTxt = br.readLine()) != null) {
					String[] names = lineTxt.split(" ");
					for (int i = 0; i < names.length - 1; i = i+2){
						loginmap.put(names[i],names[i + 1]);
					}
				}
				br.close();
			} catch (Exception e) {
				System.err.println("Read errors: " + e);
			}
			if ((loginmap.containsKey(msg1)) && msg2.equals(loginmap.get(msg1))){
				String welcomeMess = "Welcome to the greatest messaging application ever!";
				writeMsg(welcomeMess);
				broadcast(username + " logged in");
				return true;
			} else{
				String wrongPwd = "Invalid Password. Please try again";
				writeMsg(wrongPwd);
				return false;
			}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}

