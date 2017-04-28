import java.net.*;
import java.io.*;
import java.util.*;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client  {

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;

	
	// the server, the port and the username
	private String server, username;
	private int port;

	/*
	 *  Constructor called by console mode
	 *  server: the server address
	 *  port: the port number
	 */
	Client(String server, int port) {
		// which calls the common constructor with the GUI set to null
		this.server = server;
		this.port = port;
	}
	
	/*
	 * To start the dialog
	 */
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} 
		// if it failed not much I can so
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
	
		/* Creating both Data Stream */
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();

		// success we inform the caller that it worked
		return true;
	}

	/*
	 * To send a message to the console
	 */
	private void display(String msg) {
		System.out.println(msg);  
	}
	
	/*
	 * To send a message to the server
	 */
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}
	/*
	 * To send login information to the server
	 */
	void sendLogin(String msg, String pwd) {
		try {
			sOutput.writeObject(msg);
			sOutput.writeObject(pwd);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}
	/*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect not much to do in the catch clause
	 */
	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {} // not much else I can do
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {} // not much else I can do		
	}

	/*
	 * To start the Client in console mode use one of the following command
	 * > java Client username serverAddress portNumber 
	 * at the console prompt
	 * If the portNumber is not specified 1500 is used
	 * If the serverAddress is not specified "localHost" is used
	 * If the username is not specified "Anonymous" is used
	 * > java Client 
	 * is equivalent to
	 * > java Client localhost 1500
	 * are eqquivalent
	 * 
	 * In console mode, if an error occurs the program simply stops
	 */
	public static void main(String[] args) {
		// default values
		int portNumber = 1500;
		String serverAddress = "localhost";
		boolean login = false;

		switch(args.length) {
			// > javac Client serverAddr portNumber 
			case 2:
				serverAddress = args[0];
				try {
					portNumber = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					return;
				}
			// > java Client
			case 0:
				break;
			// invalid number of arguments
			default:
				System.out.println("Usage is: > java Client [serverAddress] [portNumber]");
			return;
		}
		// create the Client object
		Client client = new Client(serverAddress, portNumber);

		// if it failed nothing we can do
		if(!client.start())
			return;
		
		// prompt user to input username and password
		// and send to the server
		Scanner input = new Scanner(System.in);

		System.out.println("Username: ");
		String name = input.next();
		System.out.println("Password: ");
		String password = input.next();

		client.sendLogin(name, password); 
		
		// loop forever for command from the user
		Scanner scan = new Scanner(System.in);

		while(true) {
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			// logout if message is LOGOUT
			String[] msgsplited = msg.split(" ",2);
			String firstString = msgsplited[0];

			if(msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
				// break to do the disconnect
				break;
			}
			// message WHOELSE
			else if(msg.equalsIgnoreCase("WHOELSE")) {
				client.sendMessage(new ChatMessage(ChatMessage.WHOELSE, ""));				
			}
			// message WHOELSESINCE<time>
			else if(msg.toUpperCase().contains("WHOELSESINCE")){
				String[] msgWhoelse = msg.split(" ");
				String time = msgWhoelse[1];
				client.sendMessage(new ChatMessage(ChatMessage.WHOELSESINCE, time));
			}
			else if(firstString.equalsIgnoreCase("BROADCAST")){
				String broadCastMess = msgsplited[1];
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, broadCastMess));
			}
			else {				// default to ordinary message
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
			}
		}
		// done disconnect
		client.disconnect();	
	}

	/*
	 * a class that waits for the message from the server and append them to the JTextArea
	 * if we have a GUI or simply System.out.println() it in console mode
	 */
	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					String msg = (String) sInput.readObject();
					System.out.println(msg);
					System.out.print("> ");
				}
				catch(IOException e) {
					display("Server has close the connection: " + e);
					break;
				}
				// can't happen with a String object but need the catch anyhow
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
}
