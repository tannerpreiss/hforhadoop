import os, time


shutdown_cmd = "VBoxManage controlvm \"goldenvm\" acpipowerbutton"

restore_snap_cmd = "VBoxManage snapshot \"goldenvm\" restore gossip"

os.system(shutdown_cmd)
time.sleep(20)
os.system(restore_snap_cmd)
