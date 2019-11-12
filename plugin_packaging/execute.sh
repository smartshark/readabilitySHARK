#!/bin/sh
PLUGIN_PATH=$1
REPOSITORY_PATH=$2
NEW_UUID=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
RAMDISK_PATH="/dev/shm/$NEW_UUID"

# in case of slurm we use the provided jobid and path
if [ ! -z "$SLURM_JOB_ID" ]; then
    RAMDISK_PATH="/dev/shm/jobs/$SLURM_JOB_ID"
    rsync -q -r $REPOSITORY_PATH/ $RAMDISK_PATH || exit 1
else
  cp -R $REPOSITORY_PATH $RAMDISK_PATH
fi

cd $RAMDISK_PATH || exit 1
git checkout -f --quiet $3 || exit 1

cd $PLUGIN_PATH
COMMAND="java -jar $PLUGIN_PATH/readabilitySHARK.jar --input /dev/shm/$NEW_UUID --rev ${3} --repository_url ${4} --project_name ${5} --db_hostname ${6} --db_port ${7} --db_database ${8}"

if [ ! -z ${13+x} ] && [ ${13} != "None" ]; then
    COMMAND="$COMMAND --log_level ${13}"
fi

if [ ! -z ${9+x} ] && [ ${9} != "None" ]; then
	COMMAND="$COMMAND --db_user ${9}"
fi

if [ ! -z ${10+x} ] && [ ${10} != "None" ]; then
	COMMAND="$COMMAND --db_password ${10}"
fi

if [ ! -z ${11+x} ] && [ ${11} != "None" ]; then
	COMMAND="$COMMAND --db_authentication ${11}"
fi

if [ ! -z ${12+x} ] && [ ${12} != "None" ]; then
	COMMAND="$COMMAND -ssl"
fi

$COMMAND

# if we are on slurm this is automated (and we have no permission to remove the folder anyway)
if [ -z "$SLURM_JOB_ID" ]; then
    rm -rf $RAMDISK_PATH
fi