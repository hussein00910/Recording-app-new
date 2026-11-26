# Recording-app-new
## البناء (Build)

المشروع بهيكل Gradle القياسي (`app/` + Gradle wrapper).

### باستخدام Docker (لا يحتاج Android SDK على جهازك)

```bash
docker build -t callrecorder-builder .
docker run --rm -v "$PWD":/project callrecorder-builder
```

ينتج الملف: `app/build/outputs/apk/debug/app-debug.apk`

### باستخدام Android Studio أو SDK محلي

```bash
./gradlew assembleDebug
```
