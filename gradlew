#!/bin/sh
# Gradle start up script for POSIX generated for this project.
APP_HOME=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
APP_NAME="Gradle"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
