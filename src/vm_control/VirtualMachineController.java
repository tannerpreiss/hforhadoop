package vm_control;

/**
 * Created by tannerpreiss on 12/5/14.
 */
public class VirtualMachineController {

  private Shell shell;

  public VirtualMachineController() {
    shell = new Shell();
  }

  public boolean startVirtualMachine(VirtualMachine virtualMachine) {
    String startCmd = "VBoxHeadless --startvm \"" + virtualMachine.getVirtualMachineName() + "\"";

    String output = shell.executeCommand(startCmd);
    return true;
  }

  public boolean shutdownVirtualMachine(VirtualMachine virtualMachine) throws InterruptedException {
    //TODO: This needs to be a graceful shutdown.
    //TODO: shutdown hadoop command before virtual machine shutdown.

    String shutdownCmd = "VBoxManage controlvm " + virtualMachine.getVirtualMachineName() +
                         " acpipowerbutton";

    shell.executeCommand(shutdownCmd);

    String checkRunningVMsCmd = "VBoxManage list runningvms";

    while (!shell.executeCommand(checkRunningVMsCmd).equals("")) {
      System.out.println("Waiting for VM to graceful shutdown...");
      Thread.sleep(3);
    }
    return true;
  }

  public int notifyOfDeadNeighbor(VirtualMachine virtualMachine) {
    return 0;
  }

}
