package vm_control;

/**
 * Created by tannerpreiss on 12/5/14.
 */
public class VirtualMachineController {

   private Shell shell;

   public VirtualMachineController() {
      shell = new Shell();
   }

   public bool startVirtualMachine(VirtualMachine virtualMachine) {
      String startCmd = "VBoxHeadless --startvm \"" + virtualMachine.getVirtualMachineName()  + "\"";

      String output = shell.executeCommand(startCmd);
      return True;
   }

   public bool shutdownVirtualMachine(VirtualMachine virtualMachine) {
      //TODO: This needs to be a graceful shutdown.
      //TODO: shutdown hadoop command before virtual machine shutdown.

      String shutdownCmd = "VBoxManage controlvm " + virtualMachine.getVirtualMachineName() +
              " acpipowerbutton";

      shell.executeCommand(shutdownCmd);

      String checkRunningVMsCmd = "VBoxManage list runningvms";

      while (shell.executeCommand(checkRunningVMsCmd) != "") {
         System.out.println("Waiting for VM to graceful shutdown...");
         Thread.sleep(3);
      }

   }

   public int notifyOfDeadNeighbor(VirtualMachine virtualMachine) {

   }

}
