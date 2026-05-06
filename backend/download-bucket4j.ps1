$dest = "$env:USERPROFILE/.m2/repository/com/github/vladimir-bukhtoyarov/bucket4j-core/8.0.1"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/github/vladimir-bukhtoyarov/bucket4j-core/8.0.1/bucket4j-core-8.0.1.pom' -OutFile "$dest/bucket4j-core-8.0.1.pom"
Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/github/vladimir-bukhtoyarov/bucket4j-core/8.0.1/bucket4j-core-8.0.1.jar' -OutFile "$dest/bucket4j-core-8.0.1.jar"
Write-Host 'download complete'
