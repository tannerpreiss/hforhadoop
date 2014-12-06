package vm_control;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by tannerpreiss on 12/5/14.
 */
public class Shell {

  public String executeCommand(String command) {

    StringBuffer output = new StringBuffer();

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
}
