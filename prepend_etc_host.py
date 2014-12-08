import os,socket,subprocess

hostname = socket.gethostname()
#ip = socket.gethostbyname(hostname)
ip_cmd = "ifconfig eth0 | grep \'inet addr:\' | cut -d: -f2 | awk \'{print $1}\'"

output = subprocess.check_output(ip_cmd, shell=True)
ip = output.rstrip('\n')

host_and_ip = str(ip) + " " + str(hostname)

echo_cmd = "\"$(echo \'" + host_and_ip + "\' | cat - /etc/hosts)\""  

full_cmd = "echo " + echo_cmd + " > /etc/hosts"

#print(full_cmd)
os.system(full_cmd)

