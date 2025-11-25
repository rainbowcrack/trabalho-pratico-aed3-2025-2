#!/bin/bash
cd /home/pedrogaf/Secretária/trabalho-pratico-aed3-2025-2

# Simular entrada para testar o seed
(
echo "1"               # Iniciar sistema novo
echo "admin"           # usuário
echo "admin"           # senha
echo "5"               # menu Sistema
echo "4"               # opção Seed
echo "3"               # 3 ONGs
echo "2"               # 2 voluntários por ONG
echo "15"              # 15 adotantes
echo "10"              # 10 animais por ONG
echo "20"              # 20 interesses
echo "5"               # 5 adoções
echo "8"               # 8 threads
echo "3"               # 3 mensagens por thread
echo "s"               # confirmar
sleep 2
echo ""                # pressionar enter
echo "0"               # voltar
echo "0"               # logout
) | java -cp "Codigo/target/classes" br.com.mpet.Interface
