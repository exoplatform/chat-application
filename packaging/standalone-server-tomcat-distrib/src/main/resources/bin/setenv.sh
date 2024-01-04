#!/bin/bash -x

# Load custom settings
if [ -r "$CATALINA_BASE/bin/setenv-customize.sh" ]; then
  . "$CATALINA_BASE/bin/setenv-customize.sh"
fi

# -----------------------------------------------------------------------------
# Default JVM configuration
# -----------------------------------------------------------------------------
[ -z $EXO_JVM_VENDOR ] && EXO_JVM_VENDOR="ORACLE"
[ -z $EXO_JVM_SIZE_MAX ] && EXO_JVM_SIZE_MAX="3g"
[ -z $EXO_JVM_SIZE_MIN ] && EXO_JVM_SIZE_MIN="512m"
[ -z $EXO_JVM_METASPACE_SIZE_MAX ] && EXO_JVM_METASPACE_SIZE_MAX="512m"
[ -z $EXO_JVM_USER_LANGUAGE ] && EXO_JVM_USER_LANGUAGE="en"
[ -z $EXO_JVM_USER_REGION ] && EXO_JVM_USER_REGION="US"
[ -z $EXO_DEBUG ] && EXO_DEBUG=false
[ -z $EXO_DEBUG_PORT ] && EXO_DEBUG_PORT="8000"


# -----------------------------------------------------------------------------
# Default Logs configuration
# -----------------------------------------------------------------------------

# Default configuration for logs (using logback framework - http://logback.qos.ch/manual/configuration.html )
[ -z $EXO_LOGS_LOGBACK_CONFIG_FILE ] && EXO_LOGS_LOGBACK_CONFIG_FILE="$CATALINA_BASE/conf/logback.xml"
[ -z $EXO_LOGS_DISPLAY_CONSOLE ] && EXO_LOGS_DISPLAY_CONSOLE=false
[ -z $EXO_LOGS_COLORIZED_CONSOLE ] && EXO_LOGS_COLORIZED_CONSOLE=""

# -----------------------------------------------------------------------------
# Default Tomcat configuration
# -----------------------------------------------------------------------------

# Global Tomcat settings
[ -z $EXO_TOMCAT_UNPACK_WARS ] && EXO_TOMCAT_UNPACK_WARS=true

# -----------------------------------------------------------------------------
# Export the needed system properties for server.xml
# -----------------------------------------------------------------------------

JAVA_OPTS="$JAVA_OPTS -DEXO_TOMCAT_UNPACK_WARS=${EXO_TOMCAT_UNPACK_WARS} -DEXO_DEV=${EXO_DEV}"

# -----------------------------------------------------------------------------
# Default Tomcat configuration
# -----------------------------------------------------------------------------

# Global Tomcat settings
[ -z $EXO_TOMCAT_UNPACK_WARS ] && EXO_TOMCAT_UNPACK_WARS=true

# -----------------------------------------------------------------------------
# Export the needed system properties for server.xml
# -----------------------------------------------------------------------------

JAVA_OPTS="$JAVA_OPTS -DEXO_TOMCAT_UNPACK_WARS=${EXO_TOMCAT_UNPACK_WARS} -DEXO_DEV=${EXO_DEV}"

# -----------------------------------------------------------------------------
# Logs customization (Managed by slf4J/logback instead of tomcat-juli & co)
# -----------------------------------------------------------------------------

# Deactivate j.u.l
LOGGING_MANAGER="-Dnop"
# Add additional bootstrap entries for logging purpose using SLF4J+Logback
# SLF4J deps
if [ ! -z $CLASSPATH ]; then
  CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/slf4j-api.jar"
else
  CLASSPATH="$CATALINA_HOME/lib/slf4j-api.jar"
fi
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/jul-to-slf4j.jar"
# LogBack deps
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/logback-core.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/logback-classic.jar"
# Janino deps (used by logback for conditional processing in the config file)
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/janino.jar"
CLASSPATH="$CLASSPATH":"$CATALINA_HOME/lib/commons-compiler.jar"

# -----------------------------------------------------------------------------
# Compute the CATALINA_OPTS
# -----------------------------------------------------------------------------

if $EXO_DEBUG ; then
  CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,address=${EXO_DEBUG_PORT},server=y,suspend=n"
fi

if $EXO_DEV ; then
  CATALINA_OPTS="$CATALINA_OPTS -Dorg.exoplatform.container.configuration.debug"
  CATALINA_OPTS="$CATALINA_OPTS -Dexo.product.developing=true"
  CATALINA_OPTS="$CATALINA_OPTS -Dignore.unregistered.webapp=false"
fi

# JVM memory allocation pool parameters
CATALINA_OPTS="$CATALINA_OPTS -Xms${EXO_JVM_SIZE_MIN} -Xmx${EXO_JVM_SIZE_MAX}"

CATALINA_OPTS="$CATALINA_OPTS -XX:MaxMetaspaceSize=${EXO_JVM_METASPACE_SIZE_MAX}"

# Reduce the RMI GCs to once per hour for Sun JVMs.
CATALINA_OPTS="$CATALINA_OPTS -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"

# Default user locale defined at JVM level
CATALINA_OPTS="$CATALINA_OPTS -Duser.language=${EXO_JVM_USER_LANGUAGE} -Duser.region=${EXO_JVM_USER_REGION}"

# Network settings
CATALINA_OPTS="$CATALINA_OPTS -Djava.net.preferIPv4Stack=true"

# Headless
CATALINA_OPTS="$CATALINA_OPTS -Djava.awt.headless=true"

# Platform profiles
CATALINA_OPTS="$CATALINA_OPTS -Dexo.profiles=${EXO_PROFILES}"

# Platform paths
CATALINA_OPTS="$CATALINA_OPTS -Dexo.conf.dir=\"${EXO_CONF_DIR}\""
CATALINA_OPTS="$CATALINA_OPTS -Dgatein.conf.dir=\"${EXO_CONF_DIR}\""
CATALINA_OPTS="$CATALINA_OPTS -Djava.security.auth.login.config=\"$CATALINA_BASE/conf/jaas.conf\""
CATALINA_OPTS="$CATALINA_OPTS -Dexo.data.dir=\"${EXO_DATA_DIR}\""
# JCR Data directory
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.data.dir=\"${EXO_DATA_DIR}/jcr\""
# JCR values
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.storage.data.dir=\"${EXO_DATA_DIR}/jcr/values\""
# JCR indexes
CATALINA_OPTS="$CATALINA_OPTS -Dexo.jcr.index.data.dir=\"${EXO_DATA_DIR}/jcr/index\""
# Files storage
CATALINA_OPTS="$CATALINA_OPTS -Dexo.files.storage.dir=\"${EXO_DATA_DIR}/files\""

# Logback configuration file
CATALINA_OPTS="$CATALINA_OPTS -Dlogback.configurationFile=\"${EXO_LOGS_LOGBACK_CONFIG_FILE}\""

# Define the XML Parser depending on the JVM vendor
if [ "${EXO_JVM_VENDOR}" = "IBM" ]; then
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.stream.ZephyrWriterFactory -Djavax.xml.stream.XMLInputFactory=com.sun.xml.stream.ZephyrParserFactory -Djavax.xml.stream.XMLEventFactory=com.sun.xml.stream.events.ZephyrEventFactory"
else
  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventFactoryImpl -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
fi

# PLF-4968/JCR-2164 : Avoid Exception when starting with Java 7 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6804124)
CATALINA_OPTS="$CATALINA_OPTS -Djava.util.Arrays.useLegacyMergeSort=true"

# PLF-6550: Fix Startup problem when JVM hangs because of lack of entropy
CATALINA_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/./urandom"

# Avoid a warning on startup 
CATALINA_OPTS="$CATALINA_OPTS -Dexo.properties.url=jar:/conf/configuration.properties"

# PLF-6965 set default file encoding to UTF-8 Independently from OS default charset  CATALINA_OPTS="$CATALINA_OPTS -Djavax.xml.stream.XMLOutputFactory=com.sun.xml.internal.stream.XMLOutputFactoryImpl -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl -Djavax.xml.stream.XMLEventFactory=com.sun.xml.internal.stream.events.XMLEventFactoryImpl -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"

CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"

# Open all required modules for reflective access operations
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.io=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.lang=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.lang.invoke=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.lang.module=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/jdk.internal.module=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.math=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.net=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.nio=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.text=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.util=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.management/java.lang.management=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.desktop/java.awt.font=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.xml/com.sun.org.apache.xerces.internal.util=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens java.xml/com.sun.org.apache.xerces.internal.parsers=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS  -noverify"

export JDK_JAVA_OPTIONS
