package cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by tannerpreiss on 12/5/14.
 */
public class Shell {

  public static String executeCommand(String command) {

    StringBuilder output = new StringBuilder();

    Process p;
    try {
      p = Runtime.getRuntime().exec(command);
      p.waitFor();
      BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream()));

      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return output.toString();
  }

  public static void main(String args[]) {
    String result = Shell.executeCommand("python get_ip.py");
    System.out.println(result.trim());
  }
}
