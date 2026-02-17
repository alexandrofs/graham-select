---
name: run-test-environment
description: Instruções para subir o ambiente completo do Graham Select para testes.
---

# Subindo o Ambiente de Testes do Graham Select

Este guia descreve os passos necessários para iniciar toda a infraestrutura e os serviços do projeto Graham Select.

## Pré-requisitos
- Docker e Docker Compose instalados
- Java 22 e Maven configurados
- Flutter SDK instalado
- Portas 3306 (MySQL), 9092/9094 (Kafka), 8080 (API) e 8081 (Valuation Service) disponíveis

## Passo 1: Infraestrutura (Docker)
Inicie o banco de dados e o message broker:
```bash
docker-compose up -d
```
Verifique se os containers estão rodando:
```bash
docker ps
```

## Passo 2: Backend (Serviços Spring Boot)
O backend é composto por dois serviços que devem ser iniciados separadamente.

### 2.1. API Service
Este serviço fornece os endpoints REST para o frontend.
```bash
cd backend/api
mvn spring-boot:run -Dspring.profiles.active=local
```

### 2.2. Valuation Service
Este serviço processa as mensagens do Kafka para cálculos de valuation.
```bash
cd backend/valuation-service
mvn spring-boot:run -Dspring.profiles.active=local
```


## Passo 3: Frontend (Flutter)
Inicie a aplicação Flutter. Certifique-se de que um simulador ou dispositivo está conectado.

Para rodar no navegador (Chrome/Safari):
```bash
cd frontend
flutter run -d chrome
```

Para rodar em um dispositivo Android/iOS:
```bash
cd frontend
flutter run
```

## Verificação de Saúde (Health Checks)
- **API**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) (se configurado)
- **MySQL**: `docker exec -it graham-select-mysql mysqladmin ping -h localhost -uroot -proot`
- **Frontend**: A tela inicial deve carregar corretamente e permitir navegação.
