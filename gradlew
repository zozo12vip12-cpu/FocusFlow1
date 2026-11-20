#!/bin/sh
# Minimal Gradle wrapper launcher (Linux/macOS).
# It needs gradle/wrapper/gradle-wrapper.jar. On Codemagic that jar is created automatically
# by codemagic.yaml when it is missing (see the "Generate Gradle wrapper" step).

app_path=$0
while [ -h "$app_path" ]; do
    ls=$(ls -ld "$app_path")
    link=${ls#*' -> '}
    case $link in
        /*) app_path=$link ;;
        *) app_path=$(dirname "$app_path")/$link ;;
    esac
done
APP_HOME=$(cd "$(dirname "$app_path")" > /dev/null && pwd -P) || exit

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
    echo "ERROR: $CLASSPATH is missing." >&2
    echo "Run 'gradle wrapper --gradle-version 8.13' once, or open the project in Android Studio." >&2
    exit 1
fi

exec "$JAVACMD" -Xmx64m -Xms64m $JAVA_OPTS $GRADLE_OPTS \
    -Dorg.gradle.appname=gradlew \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
