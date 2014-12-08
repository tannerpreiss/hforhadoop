import os,time, subprocess

take_snap = False
snap_list_cmd = "VBoxManage snapshot \"goldenvm\" list | grep gossip"
take_snap_cmd = "VBoxManage snapshot \"goldenvm\" take gossip"
output = os.popen(snap_list_cmd).read()
if (output == ""):
    take_snap = True
if (take_snap):
    print("Take snap shot")
    os.system(take_snap_cmd)
    time.sleep(5)

start_vm_cmd = "VBoxManage startvm \"goldenvm\""
os.system(start_vm_cmd)
