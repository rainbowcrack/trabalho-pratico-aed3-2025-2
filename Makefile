# Detecta o sistema operacional
ifeq ($(OS),Windows_NT)
    # Windows
    SHELL := pwsh.exe -NoLogo -NoProfile -Command
    CP_SEP := ;
else
    # Linux/Mac
    SHELL := /bin/bash
    CP_SEP := :
endif

.PHONY: build run clean zip run-with-server

build:
	@echo "🔨 Compilando projeto..."
	@mvn -f Codigo/pom.xml -q -DskipTests package
	@mvn -f Codigo/pom.xml -q dependency:copy-dependencies -DoutputDirectory=target/lib
	@echo "✅ Compilação concluída!"

run:
	@echo "▶️  Iniciando CLI..."
	@java -cp "Codigo/target/classes$(CP_SEP)Codigo/target/lib/*" br.com.mpet.Interface

run-with-server:
	@echo "🚀 Iniciando servidor REST + CLI..."
	@echo "🌐 Frontend: http://localhost:8080/pages/index.html"
	@echo "🔌 API REST: http://localhost:8080/api"
	@echo ""
	@java -cp "Codigo/target/classes$(CP_SEP)Codigo/target/lib/*" br.com.mpet.InterfaceWithServer

clean:
	@echo "🧹 Limpando arquivos compilados..."
	@mvn -f Codigo/pom.xml -q clean
	@echo "✅ Limpeza concluída!"

zip:
	@echo "📦 Criando backup..."
	@java -cp "Codigo/target/classes" br.com.mpet.Interface
