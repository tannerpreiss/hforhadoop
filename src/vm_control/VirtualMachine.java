package vm_control;

/**
 * Created by tannerpreiss on 12/5/14.
 */
public class VirtualMachine {
   private String machineName;
   private String machineAddress;
   private String masterAddress;


   public VirtualMachine(String newMachineName, String newMachineAddress, String newMasterAddress)
   {
      machineName = newMachineName;
      machineAddress = newMachineAddress;
      masterAddress = newMasterAddress;
   }


   public String getVirtualMachineAddress () {
      if (machineAddress == null) {
         machineAddress = getAddressFromVBoxManage();
      }
      return machineAddress;
   }


   private String getAddressFromVBoxManage () {
      Shell tempShell = new Shell();

      String vBoxAddressCmd = "VBoxManage guestproperty get "
              + machineName + " \"/VirtualBox/GuestInfo/Net/0/V4/IP\" | " +
              "grep -o -E \'[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\'";

      return  tempShell.executeCommand(vBoxAddressCmd);
   }

   public String getVirtualMachineMasterAddress() {
      if (masterAddress == null) {
         //TODO: else should look in list of ip's and return the lowest value to
         // set as master. we need to remove null;
         return null;

      }
      return masterAddress;

   }

   public String getVirtualMachineName() {
      return machineName;
   }


}
