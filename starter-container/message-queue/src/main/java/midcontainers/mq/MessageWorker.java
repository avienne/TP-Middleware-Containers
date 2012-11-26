package midcontainers.mq;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;



public class MessageWorker extends Thread  {
	private Socket clientSocket;
	private MessageBroker msgBroker;
	private AtomicBoolean running = new AtomicBoolean(true);

	public MessageWorker(Socket socket, MessageBroker ref){
		this.clientSocket = socket;
		this.msgBroker = ref;
	}
	
	public void end(){
		this.running.set(false);
	}

	public void run(){
		try{
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

			while(running.get()){
				Operand operand = (Operand) in.readObject();

				switch (operand){
					case SEND:
					Message msg = (Message) in.readObject();
					msgBroker.addMessage(msg);
					out.writeObject(Operand.ACK);
					out.flush();
					break;

					case CHECK:
					String destination = (String) in.readObject();
					out.writeBoolean(msgBroker.checkAvailability(destination));
					out.flush();
					break;

					case RECEIVE:
					destination = (String) in.readObject();
					out.writeObject(msgBroker.getMessage(destination));
					out.flush();
					break;
				}
			}
		}catch(IOException e){
			throw new MqException(e);
		}catch (ClassNotFoundException e) {
			throw new MqException(e);
		}
	}
}