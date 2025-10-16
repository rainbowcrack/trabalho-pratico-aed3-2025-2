#!/bin/bash

# Oculta cursor
tput civis

# Símbolos de estrelinhas
STARS=('.' 'o' '*' '✦' '✧')
NUM_STARS=100

# Tamanho do terminal
cols=$(tput cols)
rows=$(tput lines)

# Armazena posições iniciais
declare -A STAR_X
declare -A STAR_Y

for ((i=0; i<NUM_STARS; i++)); do
  STAR_X[$i]=$(( RANDOM % cols ))
  STAR_Y[$i]=$(( RANDOM % rows ))
done

# Mostrar estrelinhas cintilando por 3 segundos
start_time=$(date +%s)
while (( $(date +%s) - start_time < 1 )); do
  for ((i=0; i<NUM_STARS; i++)); do
    # Apaga a estrela anterior
    tput cup ${STAR_Y[$i]} ${STAR_X[$i]}
    echo -ne " "

    # Move horizontalmente (-1, 0, +1)
    dx=$(( RANDOM % 3 - 1 ))
    STAR_X[$i]=$(( STAR_X[$i] + dx ))

    # Limites da tela
    if (( STAR_X[$i] < 0 )); then STAR_X[$i]=0; fi
    if (( STAR_X[$i] >= cols )); then STAR_X[$i]=$((cols - 1)); fi

    # Cor e simbolo aleatorios
    symbol=${STARS[$RANDOM % ${#STARS[@]}]}
    color=$(( 231 - RANDOM % 15 ))  # tons claros

    # Desenha estrela
    tput cup ${STAR_Y[$i]} ${STAR_X[$i]}
    echo -ne "\e[38;5;${color}m${symbol}\e[0m"
  done
  sleep 0.1
done

# Limpa a tela e mostra gradientes
clear
tput cnorm  # mostra cursor de volta

# Gradiente quente (texto artistico)
PALETTE=(196 202 208 214 220 226 229 231)

print_gradient() {
  local i=0 n=${#PALETTE[@]}
  while IFS= read -r line; do
    printf "\e[38;5;%sm%s\e[0m\n" "${PALETTE[i]}" "$line"
    (( i=(i+1)%n ))
  done
}

# https://ascii-art.botecodigital.dev.br/converter-texto-ascii#text

print_gradient <<'EOF'
     ___         ___                       ___           ___                       ___           ___     
    /  /\       /  /\          ___        /__/\         /  /\          ___        /  /\         /__/\    
   /  /::\     /  /:/_        /  /\      |  |::\       /  /::\        /  /\      /  /:/         \  \:\   
  /  /:/\:\   /  /:/ /\      /  /:/      |  |:|:\     /  /:/\:\      /  /:/     /  /:/           \__\:\  
 /  /:/~/:/  /  /:/ /:/_    /  /:/     __|__|:|\:\   /  /:/~/::\    /  /:/     /  /:/  ___   ___ /  /::\ 
/__/:/ /:/  /__/:/ /:/ /\  /  /::\    /__/::::| \:\ /__/:/ /:/\:\  /  /::\    /__/:/  /  /\ /__/\  /:/\:\ 
\  \:\/:/   \  \:\/:/ /:/ /__/:/\:\   \  \:\~~\__\/ \  \:\/:/__\/ /__/:/\:\   \  \:\ /  /:/ \  \:\/:/__\/
 \  \::/     \  \::/ /:/  \__\/  \:\   \  \:\        \  \::/      \__\/  \:\   \  \:\  /:/   \  \::/     
  \  \:\      \  \:\/:/        \  \:\   \  \:\        \  \:\           \  \:\   \  \:\/:/     \  \:\     
   \  \:\      \  \::/          \__\/    \  \:\        \  \:\           \__\/    \  \::/       \  \:\    
    \__\/       \__\/                     \__\/         \__\/                     \__\/         \__\/    


    "Conectando corações, unindo lares — o match perfeito entre você e um amigo de quatro patas" - PetMatch 
EOF

# Gradiente frio (arte do gato e cachorro) 
print_gradient_cool() {
  local PALETTE_COOL=(93 99 105 111 117 123 159 195 231)
  local i=0 n=${#PALETTE_COOL[@]}
  while IFS= read -r line; do
    printf "\e[38;5;%sm%s\e[0m\n" "${PALETTE_COOL[i]}" "$line"
    (( i=(i+1)%n ))
  done
}

print_gradient_cool <<'EOF'

                    /|_/|         
,_     _         o_/6 /#\\
|\\_,-~/        \\__ |##/"
/ _  _ |    ,--.  ='|--\\. '
(  Q  Q )   / ,-'     /   #'-.
\\  _T_/-._( (       \\#|_   _'-. /"
 /         ``  )       \ \\_(    )"
 /         _  \\       Cc/,--___/"  '.' 'o' '*' '✦' '✧'
 \\ \\ ,  /    |
  || |-\\__   /    Developed for AED III @ PetMatch - 2025/2 '.' 'o' '*' '✦' '✧'
 ((_/.(____,- "    '.' 'o' '*' '✦' '✧'
EOF


# roda o java

if ! command -v javac &> /dev/null # buraco negro linux
then
    echo "Erro: javac não encontrado. Instale o JDK para compilar."
    exit 1
fi

if ! command -v java &> /dev/null
then
    echo "Erro: java runtime não encontrado."
    exit 1
fi

echo "Compilando interface.java..."
javac Interface.java
if [[ $? -ne 0 ]]; then
    echo "Erro na compilação do arquivo interface.java"
    exit 1
fi

echo "Executando interface.java..."
java interface