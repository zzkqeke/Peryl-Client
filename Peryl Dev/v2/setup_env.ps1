# Set JAVA_HOME
$javaPath = "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot"
[Environment]::SetEnvironmentVariable("JAVA_HOME", $javaPath, "User")

# Add Java to PATH
$path = [Environment]::GetEnvironmentVariable("Path", "User")
if (-not $path.Contains($javaPath)) {
    [Environment]::SetEnvironmentVariable("Path", "$path;$javaPath\bin", "User")
}

Write-Host "Environment variables have been set up. Please restart your terminal for changes to take effect." 