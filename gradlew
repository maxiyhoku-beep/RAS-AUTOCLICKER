#!/bin/sh
# Minimal Gradle wrapper launcher for GitHub Actions.
# Resolves the wrapper JAR from gradle/wrapper/gradle-wrapper.jar.

DIR=$(dirname "$0")
APP_HOME=$(cd "$DIR" >/dev/null 2>&1 && pwd)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
