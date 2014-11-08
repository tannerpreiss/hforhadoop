package gossip;

import http.LogServer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Logger {
  private Node myNode;
  private ArrayList<String> events;
  public Logger(Node node) {
    myNode = node;
    events = new ArrayList<String>();

    System.out.println("Start log server");
    try {
      LogServer server = new LogServer(this);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

//    System.out.println("Start log timer");
//    TimerTask timerTask = new TimerTaskExample();
//    Timer timer = new Timer(true); // run as daemon thread
//    timer.scheduleAtFixedRate(timerTask, 0, 2000);
  }

  public void addEvent(String str) {
    synchronized (Logger.this.events) {
      events.add(str);
    }
  }

  public Node getNodeObj() { return myNode; }
  public ArrayList<String> getEvents() { return events; }

  public class TimerTaskExample extends TimerTask {

    @Override
    public void run() {
      printLogs();
    }

    // simulate a time consuming task
    private void printLogs() {
      clearConsole();
      StringBuilder str = new StringBuilder();
      str.append("Member List\n");
      str.append("===========================================\n");
      synchronized (myNode.getMembers()) {
        for (Member m : myNode.getMembers()) {
          str.append(m.toString()).append("\n");
        }
      }
      str.append("===========================================\n");
      str.append("\nEvent List\n");
      str.append("-------------------------------------------\n");
      synchronized (Logger.this.events) {
        for (int i = 0; i < events.size(); i++) {
          str.append("[").append(i).append("]")
             .append(events.get(i))
             .append("\n");
        }
      }
      str.append("-------------------------------------------\n")
         .append("\n\n\n");
      System.out.println(str.toString());
    }

    public final void clearConsole()
    {
      try
      {
        final String os = System.getProperty("os.name");

        if (os.contains("Windows"))
        {
          Runtime.getRuntime().exec("cls");
        }
        else
        {
          Runtime.getRuntime().exec("clear");
        }
      }
      catch (final Exception e)
      {
        //  Handle any exceptions.
      }
    }
  }
}
