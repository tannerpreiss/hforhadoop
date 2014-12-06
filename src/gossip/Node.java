package gossip;

import logger.Logger;

import javax.management.Notification;
import javax.management.NotificationListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Node implements NotificationListener {

  private MemberManager   memberManager;
  private DatagramSocket  gossipServer;
  private MulticastSocket multicastServer;
  private String          myAddress;
  private AtomicBoolean   inGroup;
  private ExecutorService executor;
  private Logger          log;
  private Config          config = Gossip.config;

  /**
   * Setup the client's lists, gossiping parameters, and parse the startup config file.
   *
   * @throws SocketException
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  public Node(Logger l) throws SocketException, InterruptedException, UnknownHostException {

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        System.out.println("Goodbye my friends...");
      }
    }));

    log = l;
    log.setNodeObj(this);
    memberManager = new MemberManager(this, log);
    inGroup = new AtomicBoolean(false);

    NetworkInterface networkInterface;
    InetSocketAddress address;
    InetAddress group;

    try {
      group = InetAddress.getByName(config.MULTICAST_ADDRESS);

      networkInterface = NetworkInterface.getByName(config.INTERFACE_NAME);
      address = new InetSocketAddress(group, config.MULTICAST_PORT);

      multicastServer = new MulticastSocket(config.MULTICAST_PORT);
      multicastServer.joinGroup(address, networkInterface);
    } catch (Exception e) {
      e.printStackTrace();
    }

    String myIpAddress = InetAddress.getLocalHost().getHostAddress();
    this.myAddress = myIpAddress + ":" + config.GOSSIP_PORT;

    gossipServer = new DatagramSocket(config.GOSSIP_PORT);

    executor = Executors.newCachedThreadPool();

    Member me = new Member(this.myAddress, 0, this, config.GOSSIP_CLEAN, System.currentTimeMillis());
    memberManager.addNewMember(me, true);
    log.addInfo("ADD: Add myself to the member list - " + me);
  }

  public MemberManager getMemberManager() { return memberManager; }

  public String getMyAddress() { return myAddress; }

  synchronized public void markInGroup() {
    if (!inGroup.get()) {
      inGroup.set(true);
    }
  }

  synchronized public void markOutGroup () {
    //TODO: Should we mark out of group when thread is alone?
    if (inGroup.get()) {
      inGroup.set(false);
    }
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
      while (this.keepRunning.get()) {
        try {
          TimeUnit.MILLISECONDS.sleep(config.GOSSIP_PING);
          memberManager.sendClusterInfo();
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
   * This class listens on the multicast socket for new nodes.
   * Updated the member list as necessary.
   */
  private class AsynchronousMulticastReceiver implements Runnable {

    private AtomicBoolean keepRunning;
    private Node          myNode;

    public AsynchronousMulticastReceiver(Node node) {
      keepRunning = new AtomicBoolean(true);
      myNode = node;

    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      while (keepRunning.get()) {
        DatagramPacket recv = null;
        Member newNode = null;
        try {
          do {
            // get their responses!
            byte[] buf = new byte[config.PACKET_SIZE];
            recv = new DatagramPacket(buf, buf.length);
            log.addInfo("RECV: Waiting for multicast message");

            multicastServer.receive(recv);

            // Extract member object from byte buffer
            ByteArrayInputStream bais = new ByteArrayInputStream(recv.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object read_obj = ois.readObject();
            if (read_obj instanceof Member) {
              newNode = (Member)read_obj;
              if (newNode.getAddress().equals(myAddress)) {
                log.addInfo("RECV: Received multicast message (FROM ME) - " + newNode);
              } else {
                log.addInfo("RECV: Received multicast message - " + newNode);
              }
            }
            newNode = (Member)read_obj;
            if (newNode.getAddress().equals(myAddress)) {
              log.addInfo("RECV: Received multicast message (FROM ME) - " + newNode);
            } else {
              log.addInfo("RECV: Received multicast message - " + newNode);
            }
          } while (newNode == null || newNode.getAddress().equals(myAddress));
        } catch (IOException e) {
          log.addError("Error receiving datagram packet: " + e.getMessage());
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          log.addError(e.getMessage());
          e.printStackTrace();
        }

        markInGroup(); // Change to inGroup state
        memberManager.addNewMember(newNode, false); // Add new member to list
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
  class AsynchronousGossipReceiver implements Runnable {

    private AtomicBoolean keepRunning;
    private Node          myNode;

    public AsynchronousGossipReceiver() {
      keepRunning = new AtomicBoolean(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      while (keepRunning.get()) {
        try {
          byte[] buf = new byte[config.PACKET_SIZE];
          DatagramPacket p = new DatagramPacket(buf, buf.length);
          gossipServer.receive(p);

          // Extract the member array list out of the packet
          // TODO: maybe abstract this out to pass just the bytes needed
//          ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
//          ObjectInputStream ois = new ObjectInputStream(bais);
          ClusterInfo info = ClusterInfo.deserializeClusterInfo(p.getData());
          log.addInfo("RECV: Receiving member list\nFrom: " +
                     p.getAddress() + "\n" +
                     info.toString());
          markInGroup();
          memberManager.mergeLists(info);

//          Object readObject = ois.readObject();
//          if (readObject instanceof ArrayList<?>) {
//            ArrayList<Member> list = (ArrayList<Member>) readObject;
//
//            StringBuilder str = new StringBuilder();
//            str.append("RECV: Receiving member list\n From: ")
//               .append(p.getAddress()).append("\nList:\n");
//            for (Member m : list) {
//              str.append(m).append("\n");
//            }
//            log.addInfo(str.toString());
//
//            markInGroup();
//            // Merge our list with the one we just received
//            memberManager.mergeLists(list);
//          }

        } catch (IOException e) {
          log.addError("Gossip receiver IO error. Stopping thread...");
          e.printStackTrace();
          keepRunning.set(false);
        } catch (ClassNotFoundException e) {
          log.addError(e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Starts the client.  Specifically, start the various cycles for this protocol.
   * Start the gossip thread and start the receiver thread.
   *
   * @throws InterruptedException
   */
  public void start_listeners(Node node) throws InterruptedException {

    //  The receiver thread is a passive player that handles
    //  merging incoming membership lists from other neighbors.
    executor.execute(new AsynchronousGossipReceiver());

    // AsynchronousMulticastReceiver()
    executor.execute(new AsynchronousMulticastReceiver(node));

    while (!inGroup.get()) {
      node.send_multicast();
    }

    log.addInfo("IN GROUP!");
    executor.execute(new MembershipGossiper());

    // keep the main thread around
    while (true) {
      TimeUnit.SECONDS.sleep(10);
    }
  }

  private void send_multicast() {
    // join a Multicast group and send the group salutations
    try {
      // Serialize member object into bytes
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(memberManager.getMe());

      byte[] buf = baos.toByteArray();
      if (buf.length > config.PACKET_SIZE) {
        log.addError("Member object is larger than packet size");
      }

      // Send member object to multicast address
      InetAddress group = InetAddress.getByName(config.MULTICAST_ADDRESS);
      DatagramPacket member_obj = new DatagramPacket(buf, buf.length, group, config.MULTICAST_PORT);
      log.addInfo("SEND: Sent multicast message");
      multicastServer.send(member_obj);
      Thread.sleep(config.MULTICAST_WAIT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * All timers associated with a member will trigger this method when it goes
   * off.  The timer will go off if we have not heard from this member in
   * <code> GOSSIP_CLEAN </code> time.
   */
  @Override
  public void handleNotification(Notification notification, Object handback) {

    Member deadMember = (Member) notification.getUserData();

    log.addInfo("DEAD: Dead member detected - " + deadMember);

    memberManager.killMember(deadMember);
  }
}
