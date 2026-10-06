#!/bin/sh
# ============================================================================
#  不死图腾名称动画 - 构建入口（macOS / Linux / Git Bash）
#
#  用法：
#      ./gradlew           -> 构建 jar
#      ./gradlew runClient -> 直接启动游戏调试
#
#  优先使用本机已有的 Gradle，找不到才回退到官方 wrapper（需要联网）。
# ============================================================================

PROJECT_DIR=$(cd "$(dirname "$0")" && pwd)

# ---- 1) 确保有 JDK ----
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; then
    echo "[错误] 请设置 JAVA_HOME 指向 JDK（需包含 bin/javac 与 jmods/）。"
    echo "       例如：  export JAVA_HOME=\$(/usr/libexec/java_home -v 21)   # macOS"
    exit 1
fi

# ---- 2) Gradle 缓存目录 ----
if [ -z "$GRADLE_USER_HOME" ]; then
    if [ -d "$PROJECT_DIR/../.gradle-home" ]; then
        GRADLE_USER_HOME="$PROJECT_DIR/../.gradle-home"
    else
        GRADLE_USER_HOME="$PROJECT_DIR/.gradle-home"
    fi
    export GRADLE_USER_HOME
fi

# ---- 3) 找一个可用的 Gradle ----
GRADLE_BIN=""
if [ -n "$GRADLE_HOME" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
    GRADLE_BIN="$GRADLE_HOME/bin/gradle"
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN=$(command -v gradle)
elif [ -x "$PROJECT_DIR/.gradle-local/bin/gradle" ]; then
    GRADLE_BIN="$PROJECT_DIR/.gradle-local/bin/gradle"
fi

echo "[信息] JAVA_HOME        = $JAVA_HOME"
echo "[信息] GRADLE_USER_HOME = $GRADLE_USER_HOME"

if [ -n "$GRADLE_BIN" ]; then
    echo "[信息] 使用本地 Gradle    = $GRADLE_BIN"
    echo
    exec "$GRADLE_BIN" --no-daemon --console=plain -p "$PROJECT_DIR" "$@"
else
    echo "[信息] 未找到本地 Gradle，回退到官方 wrapper（首次运行需联网下载约 130MB）..."
    echo
    exec java -Xmx64m -Xms64m \
        -Dorg.gradle.appname=gradlew \
        -classpath "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" \
        org.gradle.wrapper.GradleWrapperMain "$@"
fi
