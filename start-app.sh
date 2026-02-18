#!/bin/bash

# Cores para o output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Graham Select - Inicializador Local ===${NC}"

# 1. Subir Infraestrutura Docker
echo -e "${GREEN}[1/4] Iniciando infraestrutura (MySQL & Kafka)...${NC}"
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao subir os containers Docker. Verifique se o Docker Desktop está rodando.${NC}"
    exit 1
fi

# 2. Instalar Módulo Common
echo -e "${GREEN}[2/4] Instalando módulo common...${NC}"
cd backend && mvn clean install -pl common -am -DskipTests
cd ..

# 3. Iniciar Serviços Backend e Frontend em Paralelo
echo -e "${GREEN}[3/4] Iniciando Serviços Backend e Frontend...${NC}"

# Função para finalizar processos ao sair
cleanup() {
    echo -e "\n${RED}Finalizando serviços...${NC}"
    kill $API_PID $VALUATION_PID $FRONT_PID
    docker-compose down
    exit
}

trap cleanup SIGINT

echo -e "${BLUE}Iniciando API Service na porta 8080...${NC}"
cd backend && mvn spring-boot:run -pl api -Dspring-boot.run.profiles=local > ../api.log 2>&1 &
API_PID=$!

echo -e "${BLUE}Iniciando Valuation Service na porta 8081...${NC}"
cd backend && mvn spring-boot:run -pl valuation-service -Dspring-boot.run.profiles=local > ../valuation.log 2>&1 &
VALUATION_PID=$!

echo -e "${BLUE}Iniciando Frontend (Flutter Web)...${NC}"
cd frontend && flutter run -d chrome --web-port 3000 > ../frontend.log 2>&1 &
FRONT_PID=$!


# 4. Monitoramento
echo -e "${GREEN}[4/4] Ambiente subindo!${NC}"
echo -e "Logs disponíveis em: ${BLUE}api.log${NC}, ${BLUE}valuation.log${NC} e ${BLUE}frontend.log${NC}"
echo -e "Pressione ${RED}CTRL+C${NC} para parar todos os serviços."

# Aguarda os processos
wait
