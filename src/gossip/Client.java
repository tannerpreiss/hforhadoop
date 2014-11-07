package gossip;

import javax.management.Notification;
import javax.management.NotificationListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements NotificationListener {

	private ArrayList<Member>   memberList;
	private ArrayList<Member>   deadList;
	private int                 t_gossip;   //in ms
	public  int                 t_cleanup;  //in ms
	private Random              random;
	private DatagramSocket      gossipServer;
	private MulticastSocket     multicastServer;
	private String              myAddress;
	private Member              me;
	private AtomicBoolean       inGroup;
	private ExecutorService     executor;

    // Globals for IP Communication
    private final int           MULTICAST_PORT      = 6789;
    private final String        MULTICAST_ADDRESS   = "228.5.6.7";
    private final String        INTERFACE_NAME      = "en1";

	/**
	 * Setup the client's lists, gossiping parameters, and parse the startup config file.
	 * @throws SocketException
	 * @throws InterruptedException
	 * @throws UnknownHostException
	 */
	public Client() throws SocketException, InterruptedException, UnknownHostException {

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				System.out.println("Goodbye my friends...");
			}
		}));

		memberList  = new ArrayList<Member>();
		deadList    = new ArrayList<Member>();
		t_gossip    = 100;                      // .1 second
		t_cleanup   = 10000;                    // 10 seconds
		random      = new Random();
    inGroup = new AtomicBoolean(false);
    int port    = 2222;

		NetworkInterface networkInterface;
		InetSocketAddress address;
		InetAddress group;

        try {
			group = InetAddress.getByName(MULTICAST_ADDRESS);

			networkInterface = NetworkInterface.getByName(INTERFACE_NAME);
			address = new InetSocketAddress(group, MULTICAST_PORT);

			multicastServer = new MulticastSocket(MULTICAST_PORT);
			multicastServer.joinGroup(address, networkInterface);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		String myIpAddress = InetAddress.getLocalHost().getHostAddress();
		this.myAddress = myIpAddress + ":" + port;

		gossipServer = new DatagramSocket(port);

		executor = Executors.newCachedThreadPool();

		me = new Member(this.myAddress, 0, this, t_cleanup);
    me.setAsMe();
		System.out.println("I am " + me);
		memberList.add(me);
	}

	/**
	 * Performs the sending of the membership list, after we have
	 * incremented our own heartbeat.
	 */
	private void sendMembershipList() {

		this.me.setHeartbeat(me.getHeartbeat() + 1);

		synchronized (this.memberList) {
			try {
				Member member = getRandomMember();

				if(member != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(this.memberList);
					byte[] buf = baos.toByteArray();

					String address = member.getAddress();
					String host = address.split(":")[0];
					int port = Integer.parseInt(address.split(":")[1]);

					InetAddress dest;
					dest = InetAddress.getByName(host);

					System.out.println("Sending to " + dest);
					System.out.println("---------------------");
					for (Member m : memberList) {
						System.out.println(m);
					}
					System.out.println("---------------------");

					//simulate some packet loss ~25%
					int percentToSend = random.nextInt(100);
					if(percentToSend > 25) {
						DatagramSocket socket = new DatagramSocket();
						DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
						socket.send(datagramPacket);
						socket.close();
					}
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Find a random peer from the local membership list.
	 * Ensure that we do not select ourselves, and keep
	 * trying 10 times if we do.  Therefore, in the case 
	 * where this client is the only member in the list, 
	 * this method will return null
	 * @return Member random member if list is greater than 1, null otherwise
	 */
	private Member getRandomMember() {
		Member member = null;

		if(this.memberList.size() > 1) {
			int tries = 10;
			do {
				int randomNeighborIndex = random.nextInt(this.memberList.size());
				member = this.memberList.get(randomNeighborIndex);
				if(--tries <= 0) {
					member = null;
					break;
				}
			} while(member.getAddress().equals(this.myAddress));
		}
		else {
			System.out.println("I am alone in this world.");
		}

		return member;
	}

	/**
	 * The class handles gossiping the membership list.
	 * This information is important to maintaining a common
	 * state among all the nodes, and is important for detecting
	 * failures.
	 */
	private class MembershipGossiper implements Runnable {

		private AtomicBoolean keepRunning;

		public MembershipGossiper() {
			this.keepRunning = new AtomicBoolean(true);
		}

		@Override
		public void run() {
			while(this.keepRunning.get()) {
				try {
					TimeUnit.MILLISECONDS.sleep(t_gossip);
					sendMembershipList();
				} catch (InterruptedException e) {
					// TODO: handle exception
					// This membership thread was interrupted externally, shutdown
					e.printStackTrace();
					keepRunning.set(false);
				}
			}

			this.keepRunning = null;
		}

	}

	/**
	 * This class handles the passive cycle, where this client
	 * has received an incoming message.  For now, this message
	 * is always the membership list, but if you choose to gossip
	 * additional information, you will need some logic to determine
	 * the incoming message.
	 */
	private class AsynchronousMulticastReceiver implements Runnable {

		private AtomicBoolean keepRunning;
		private Client myClient;

		public AsynchronousMulticastReceiver(Client client) {
			keepRunning = new AtomicBoolean(true);
			myClient = client;

		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			while (keepRunning.get()) {

				// --------------------- RECEIVE ON MULTICAST ---------------------


				DatagramPacket recv = null;
				String newNodeIp = "";
				System.out.println("HIIIIII");
				try {
					do {
						// get their responses!
						byte[] buf = new byte[1000];
						recv = new DatagramPacket(buf, buf.length);
						System.out.println("started to block on receive!");

						multicastServer.receive(recv);

						newNodeIp = recv.getAddress().toString().substring(1) + ":2222";
						System.out.println(newNodeIp);

					} while (((newNodeIp).equals(myAddress)));
				} catch (Exception e) {
					e.printStackTrace();
				}
				// ----------------------------------------------------------------


				// ------------------------ ADD IP TO LIST ------------------------
				// String newNodeIp = recv.getAddress().toString().substring(1) + ":2222";
				System.out.println("ADDING: " + newNodeIp);

				Member newNode = new Member(newNodeIp, 0, myClient, t_cleanup);
				System.out.println("Adding new node: " + newNode);
        synchronized (Client.this.inGroup) {
          if (!inGroup.get()) {
            inGroup.set(true);
          }
        }
        ArrayList<Member> new_member = new ArrayList<Member>();
        new_member.add(newNode);
        mergeLists(new_member);
			}
		}
	}

	/**
	 * This class handles the passive cycle, where this client
	 * has received an incoming message.  For now, this message
	 * is always the membership list, but if you choose to gossip
	 * additional information, you will need some logic to determine
	 * the incoming message.
	 */
	private class AsynchronousGossipReceiver implements Runnable {

		private AtomicBoolean keepRunning;
    private Client myClient;

		public AsynchronousGossipReceiver() {
			keepRunning = new AtomicBoolean(true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			while(keepRunning.get()) {
				try {
					//XXX: be mindful of this array size for later
					byte[] buf = new byte[1000];
					DatagramPacket p = new DatagramPacket(buf, buf.length);
					gossipServer.receive(p);
					// extract the member arraylist out of the packet
					// TODO: maybe abstract this out to pass just the bytes needed
					ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
					ObjectInputStream ois = new ObjectInputStream(bais);

					Object readObject = ois.readObject();
					if(readObject instanceof ArrayList<?>) {
//						inGroup = true;
						ArrayList<Member> list = (ArrayList<Member>) readObject;

						System.out.println("Received member list:");
						for (Member member : list) {
							System.out.println(member);
						}
            synchronized (Client.this.inGroup) {
              if (!inGroup.get()) {
                inGroup.set(true);
              }
            }
            // Merge our list with the one we just received
            mergeLists(list);
					}

				} catch (IOException e) {
					e.printStackTrace();
					keepRunning.set(false);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Starts the client.  Specifically, start the various cycles for this protocol.
	 * Start the gossip thread and start the receiver thread.
	 * @throws InterruptedException
	 */
	private void start_listeners(Client client) throws InterruptedException {

        // TODO: this will not run, since in the constructor, all we add is oneself
		for (Member member : memberList) {
			if (member != me) {
				member.startTimeoutTimer();
			}
		}

		//  The receiver thread is a passive player that handles
		//  merging incoming membership lists from other neighbors.
		executor.execute(new AsynchronousGossipReceiver());

		// AsynchronousMulticastReceiver()
		executor.execute(new AsynchronousMulticastReceiver(client));

    while (!inGroup.get()) {
      client.send_multicast();
		}

		System.out.println("IN GROUP!!!!!!!!!");
		executor.execute(new MembershipGossiper());

		// keep the main thread around
		while(true) {
			TimeUnit.SECONDS.sleep(10);
		}
	}

	private void send_multicast() {
		// join a Multicast group and send the group salutations
		try {
      String msg = "Hello";
      InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
			DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, MULTICAST_PORT);
			multicastServer.send(hi);
			Thread.sleep(3000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

  /**
   * Merge remote list (received from peer), and our local member list.
   * Simply, we must update the heartbeats that the remote list has with
   * our list.  Also, some additional logic is needed to make sure we have
   * not timed out a member and then immediately received a list with that 
   * member.
   * @param remoteList
   */
  private void mergeLists(ArrayList<Member> remoteList) {

    synchronized (Client.this.deadList) {

      synchronized (Client.this.memberList) {

        for (Member remoteMember : remoteList) {
          if(Client.this.memberList.contains(remoteMember)) {
            Member localMember = Client.this.memberList.get(Client.this.memberList.indexOf(remoteMember));

            if(remoteMember.getHeartbeat() > localMember.getHeartbeat()) {
              // update local list with latest heartbeat
              localMember.setHeartbeat(remoteMember.getHeartbeat());
              // and reset the timeout of that member
              localMember.resetTimeoutTimer();
            }
          }
          else {
            // the local list does not contain the remote member

            // the remote member is either brand new, or a previously declared dead member
            // if its dead, check the heartbeat because it may have come back from the dead

            if(Client.this.deadList.contains(remoteMember)) {
              Member localDeadMember = Client.this.deadList.get(Client.this.deadList.indexOf(remoteMember));
              if(remoteMember.getHeartbeat() > localDeadMember.getHeartbeat()) {
                // it's baa-aack
                Client.this.deadList.remove(localDeadMember);
                Member newLocalMember = new Member(remoteMember.getAddress(), remoteMember.getHeartbeat(), Client.this, t_cleanup);
                Client.this.memberList.add(newLocalMember);
                newLocalMember.startTimeoutTimer();
              } // else ignore
            }
            else {
              // brand spanking new member - welcome
              Member newLocalMember = new Member(remoteMember.getAddress(), remoteMember.getHeartbeat(), Client.this, t_cleanup);
              Client.this.memberList.add(newLocalMember);
              newLocalMember.startTimeoutTimer();
            }
          }
        }
      }
    }
  }

	public static void main(String[] args) throws InterruptedException, SocketException, UnknownHostException
    {
		Client client = new Client();
		client.start_listeners(client);
	}

	/**
	 * All timers associated with a member will trigger this method when it goes
	 * off.  The timer will go off if we have not heard from this member in
	 * <code> t_cleanup </code> time.
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {

		Member deadMember = (Member) notification.getUserData();

		System.out.println("Dead member detected: " + deadMember);

		synchronized (this.memberList) {
			this.memberList.remove(deadMember);
		}

		synchronized (this.deadList) {
			this.deadList.add(deadMember);
		}
	}
}
