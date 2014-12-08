import os, subprocess

ip_cmd = "VBoxManage guestproperty get goldenvm \"/VirtualBox/GuestInfo/Net/0/V4/IP\" | grep -o -E '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}'"

# print(ip_cmd)
os.system(ip_cmd)
# output = subprocess.check_output(ip_cmd, shell=True)
#ip = output.rstrip('\n')
