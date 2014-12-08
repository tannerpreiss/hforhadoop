import fileinput, shutil, argparse


argparser = argparse.ArgumentParser(description="Use random sampling to find a motif.")
argparser.add_argument("-m", "--master", dest="master", type=str, help="The ip address of the master node")
args = argparser.parse_args()

prev_master_name = "@MASTER_HOSTNAME"
new_master_name = args.master;

golden_paths = ["/home/frosty/golden-core-site.xml",
				  "/home/frosty/golden-yarn-site.xml"]

dest_paths = ["/usr/local/hadoop/etc/hadoop/core-site.xml",
			  "/usr/local/hadoop/etc/hadoop/yarn-site.xml"]

#Copy the golden copy files into the final destination for Hadoop.
for i in range(0, len(dest_paths)):
	shutil.copy2(golden_paths[i], dest_paths[i])

#Find and replace all 
for line in fileinput.input(dest_paths, inplace=True):
	print(line.replace(prev_master_name, new_master_name), end='')
