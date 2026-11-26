# Build environment for the Android app: JDK 17 + Android SDK command-line tools
ARG BASE_IMAGE=eclipse-temurin:17-jdk
FROM ${BASE_IMAGE}
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl && rm -rf /var/lib/apt/lists/*
RUN mkdir -p $ANDROID_HOME/cmdline-tools && cd /tmp \
 && curl -fsSL -o tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
 && unzip -q tools.zip && mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest && rm tools.zip
RUN yes | sdkmanager --licenses >/dev/null \
 && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
WORKDIR /project
CMD ["./gradlew", "assembleDebug", "--no-daemon", "--stacktrace"]
