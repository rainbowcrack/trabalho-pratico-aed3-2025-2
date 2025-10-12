SHELL := pwsh.exe -NoLogo -NoProfile -Command

.PHONY: build run clean zip

build:
	#mvn wrapper ou mvn normal, ajuste conforme seu ambiente
	mvn -f Codigo/pom.xml -q -DskipTests package

run:
	# Executa a Interface via classpath de classes compiladas
	java -cp "Codigo/target/classes" br.com.mpet.Interface

clean:
	mvn -f Codigo/pom.xml -q clean

zip:
	# Gera backup via a própria Interface (opção 9)
	java -cp "Codigo/target/classes" br.com.mpet.Interface
