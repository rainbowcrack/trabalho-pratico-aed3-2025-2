SHELL := pwsh.exe -NoLogo -NoProfile -Command

.PHONY: build run clean zip run-with-server

build:
	#mvn wrapper ou mvn normal, ajuste conforme seu ambiente
	mvn -f Codigo/pom.xml -q -DskipTests package

run:
	# Executa a Interface via classpath de classes compiladas
	java -cp "Codigo/target/classes" br.com.mpet.Interface

run-with-server:
	# Executa a Interface + REST Server em localhost:8080
	# O frontend pode ser acessado via http://localhost:8080/pages/index.html
	java -cp "Codigo/target/classes" br.com.mpet.InterfaceWithServer

clean:
	mvn -f Codigo/pom.xml -q clean

zip:
	# Gera backup via a própria Interface (opção 9)
	java -cp "Codigo/target/classes" br.com.mpet.Interface
