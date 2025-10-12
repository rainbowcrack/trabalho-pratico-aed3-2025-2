# Build and run Interface using PowerShell
$mvn = "mvn"
& $mvn -f "Codigo/pom.xml" -q -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java -cp "Codigo/target/classes" br.com.mpet.Interface
