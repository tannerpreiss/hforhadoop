import os, time


shutdown_cmd = "VBoxManage controlvm \"goldenvm\" acpipowerbutton"

restore_snap_cmd = "VBoxManage snapshot \"goldenvm\" restore master-state"

# delete_cmd = "VBoxManage snapshot \"goldenvm\" delete master-state"

os.system(shutdown_cmd)
time.sleep(20)
os.system(restore_snap_cmd)

