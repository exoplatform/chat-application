#!/bin/bash
#
# Utility script to ease the Development process a little bit ;-)
#
# Successfully tested with :
#   - Platform 3.5.5 + Mac OSX 10.8
#   -

SCRIPT_LAUNCH_DIR=$(pwd)
PROJECT_DIR=$(cd $(dirname "$0"); pwd)
PLF_DEFAULT_TOMCAT_DIRECTORY=${PLF_TOMCAT_DIRECTORY:-"$PROJECT_DIR/tomcat"}
PLF_DEFAULT_DATA_DIRECTORY=${PLF_DATA_DIRECTORY:-"$PLF_DEFAULT_TOMCAT_DIRECTORY/gatein/data"}

function script_usage {
	local ERRMSG=$1
	if [ ! -z "$ERRMSG" ];then echo "# ERROR # ${ERRMSG}"; fi
	echo "Script usage : "
	echo "    $0 [-r] [-c] [-t TOMCAT_HOME_DIRECTORY_PATH] [-d PLF_DATA_DIRECTORY_PATH] [-w PLF_WEBAPP_DIRECTORY_PATH]"
	echo "  "
	echo "       -r              start or restart the Tomcat server     (default: do nothing)"
	echo "       -m              build / rebuild with maven             (default: do nothing)"
	echo "       -u              update the project binaries in Tomcat  (default: do nothing)"
	echo "       -c              cleanup the Tomcat Platform            (default: do nothing)"
	echo "                         (data + logs + temp)"
	echo "       -t <path>       the Platform Tomcat directory          (default: $PLF_DEFAULT_TOMCAT_DIRECTORY)"
	echo "       -d <path>       the Platform data directory            (default: <TOMCAT-DIR>/gatein/data)"
	echo "                       "
	echo "    You can combine the r / m / u / c switches together to chain several actions."
	echo "    For exemple you can rebuild the project, update the binaries and restart the tomcat with the following command :"
  echo "       $0 -m -u -r"
}

while getopts "rmuct:d:h" OPTION; do
	case $OPTION in
		r)
			ACTION_RESTART=true
			;;
		m)
			ACTION_MAVEN=true
			;;
		u)
			ACTION_UPDATE=true
			;;
		c)
			ACTION_CLEAN=true
			;;
		d)
			PLF_DATA_DIRECTORY=$OPTARG
			;;
		t)
			PLF_TOMCAT_DIRECTORY=$OPTARG
			;;
		h)
			script_usage
			exit 1
			;;
		?)
			script_usage "Invalid option \"$OPTARG\""
			exit 1
			;;
		esac
done

PLF_TOMCAT_DIRECTORY=${PLF_TOMCAT_DIRECTORY:-"$PLF_DEFAULT_TOMCAT_DIRECTORY"}
PLF_WEBAPP_DIRECTORY=${PLF_TOMCAT_DIRECTORY}/webapps
PLF_DATA_DIRECTORY=${PLF_DATA_DIRECTORY:-"$PLF_TOMCAT_DIRECTORY/gatein/data"}

##############################
# Stop the application server if needed
if [[ ! -z ${ACTION_RESTART} && ${ACTION_RESTART} == "true" ]]; then
  echo "# Stoping the Tomcat server ..."
#  if [[ -f ${PLF_TOMCAT_DIRECTORY}/temp/catalina.pid ]]; then
  pushd ${PLF_TOMCAT_DIRECTORY}/bin
  ${PLF_TOMCAT_DIRECTORY}/bin/catalina.sh stop 10 -force
  popd
#  else
#    echo ""
#  fi
else
  echo "# The Tomcat server will not be restarted"
  echo "  (add the -r switch if needed)"
fi

##############################
# Rebuild the project with Maven if needed
if [[ ! -z ${ACTION_MAVEN} && ${ACTION_MAVEN} == "true" ]]; then
  pushd ${PROJECT_DIR}
  mvn clean install
  popd
else
  echo "# The project binaries will not be rebuilt with maven"
  echo "  (add the -u switch if needed)"
fi

##############################
# cleanup the Tomcat Platform (log + data + temp)
if [[ ! -z ${ACTION_CLEAN} && ${ACTION_CLEAN} == "true" ]]; then
  echo "# Cleaning the Platform ..."
  echo    "  - Removing the data directory   : "
  echo -n "       rm -rf ${PLF_DATA_DIRECTORY}"
  if [ -d "${PLF_DATA_DIRECTORY}" ] ; then
#    rm -R ${PLF_DATA_DIRECTORY}
    echo " ... OK"
  else
    echo " ... no existing data => SKIP"
  fi
  echo    "  - Cleaning the logs directory   : "
  echo -n "       rm -rf ${PLF_TOMCAT_DIRECTORY:-$PLF_DEFAULT_TOMCAT_DIRECTORY}/logs/*"
    echo " ... OK"
  echo -n "       rm -rf ${PLF_DATA_DIRECTORY}"
    echo " ... OK"
  echo    "  - Cleaning the temp directory   : "
  echo -n "       rm -rf ${PLF_TOMCAT_DIRECTORY:-$PLF_DEFAULT_TOMCAT_DIRECTORY}/temp/*"
    echo " ... OK"
fi

##############################
# Deploy the needed binaries

if [[ ! -z ${ACTION_UPDATE} && ${ACTION_UPDATE} == "true" ]]; then
  # Removing old binaries
  echo "# Deploying the Blog binary ..."
  echo    "  - Cleaning the webapp directory : "
  echo -n "         rm -rf ${PLF_WEBAPP_DIRECTORY}/blog/"
  if [ -d "${PLF_WEBAPP_DIRECTORY}/blog/" ] ; then
    rm -R ${PLF_WEBAPP_DIRECTORY}/blog/
    echo " ... OK"
  else
    echo " ... no existing deployed war => SKIP"
  fi
  echo -n "         rm -f ${PLF_WEBAPP_DIRECTORY}/blog.war"
  if [ -f "${PLF_WEBAPP_DIRECTORY}/blog.war" ] ; then
    rm -f ${PLF_WEBAPP_DIRECTORY}/blog.war
    echo " ... OK"
  else
    echo " ... no existing war => SKIP"
  fi
  echo -n "         rm -fv ${PLF_TOMCAT_DIRECTORY}/lib/blog-config-*.jar"
  rm -fv ${PLF_TOMCAT_DIRECTORY}/lib/blog-config-*.jar
  echo " ... OK"

  # Deploying new binaries
  echo    "  - Adding the new version : "
  echo -n "         cp -vp ${PROJECT_DIR}/config/target/blog-config-*.jar ${PLF_TOMCAT_DIRECTORY}/lib/"
  if [ -d "${PLF_TOMCAT_DIRECTORY}/lib/" ] ; then
    cp -vp ${PROJECT_DIR}/config/target/blog-config-*.jar ${PLF_TOMCAT_DIRECTORY}/lib/
    echo " ... OK"
  else
    echo " ... no existing deployed war => SKIP"
  fi
  echo -n "         cp -vp ${PROJECT_DIR}/webapp/target/blog.war ${PLF_WEBAPP_DIRECTORY}/"
  if [ -d "${PLF_WEBAPP_DIRECTORY}/" ] ; then
    cp -vp ${PROJECT_DIR}/webapp/target/blog.war ${PLF_WEBAPP_DIRECTORY}/
    echo " ... OK"
  else
    echo " ... no existing deployed war => SKIP"
  fi
else
  echo "# The project binaries will not be redeployed"
  echo "  (add the -u switch if needed)"
fi

##############################
# Start the application server if needed
if [[ ! -z ${ACTION_RESTART} && ${ACTION_RESTART} == "true" ]]; then
  echo "# Starting the Tomcat server ... (wait some time until Tomcat is started)"
  pushd ${PLF_TOMCAT_DIRECTORY}/bin
  ${PLF_TOMCAT_DIRECTORY}/bin/catalina.sh start
  popd
  sleep 1
  tail -f ${PLF_TOMCAT_DIRECTORY}/logs/catalina.out
else
  echo "# The Tomcat server will not be restarted"
  echo "  (add the -r switch if needed)"
fi
