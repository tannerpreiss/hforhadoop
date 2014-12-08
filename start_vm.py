import os,time

take_snap_cmd = "VBoxManage snapshot \"goldenvm\" take master-state"
start_vm_cmd = "VBoxManage startvm \"goldenvm\""

os.system(take_snap_cmd)
time.sleep(5)
os.system(start_vm_cmd)
# time.sleep(25)
# os.system(take_snap_cmd)
