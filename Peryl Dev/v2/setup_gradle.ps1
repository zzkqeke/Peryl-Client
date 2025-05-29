# Create gradle wrapper directory if it doesn't exist
New-Item -ItemType Directory -Force -Path "gradle\wrapper"

# Download gradle-wrapper.jar
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar" -OutFile "gradle\wrapper\gradle-wrapper.jar"

# Download gradle-wrapper.properties
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.properties" -OutFile "gradle\wrapper\gradle-wrapper.properties"

# Download gradlew
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradlew" -OutFile "gradlew"

# Download gradlew.bat
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradlew.bat" -OutFile "gradlew.bat"

Write-Host "Gradle wrapper has been set up. You can now run gradlew.bat commands." 