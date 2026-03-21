---
name: run-test-environment
description: Instruções para subir o ambiente completo do Graham Select para testes.
---

# Subindo o Ambiente de Testes do Graham Select

Este guia descreve os passos necessários para iniciar toda a infraestrutura e os serviços do projeto Graham Select utilizando o script de inicialização automatizada.

## Pré-requisitos
- Docker e Docker Compose instalados
- Java 22 e Maven configurados
- Flutter SDK instalado
- Portas 3306 (MySQL), 9092/9094 (Kafka), 8080 (API) e 8081 (Valuation Service) disponíveis

## Passo Único: Executar o Script de Inicialização

O projeto possui um script centralizado que sobe a infraestrutura (Docker), instala dependências e inicia os serviços de backend e frontend em paralelo.

1. Certifique-se de que o script tem permissão de execução:
   ```bash
   chmod +x start-app.sh
   ```

2. Execute o script:
   ```bash
   ./start-app.sh
   ```

3. O script irá:
   - Iniciar MySQL e Kafka via Docker Compose.
   - Instalar o módulo `common`.
   - Iniciar a **API** (Porta 8080).
   - Iniciar o **Valuation Service** (Porta 8081).
   - Iniciar o **Frontend** (Flutter Web na porta 3000).

## Monitoramento de Logs

Como os serviços rodam em background, você pode acompanhar os logs em arquivos separados:
- **API**: `tail -f api.log`
- **Valuation**: `tail -f valuation.log`
- **Frontend**: `tail -f frontend.log`

Para parar todos os serviços, pressione `CTRL+C` no terminal onde o script foi executado.

## Verificação de Saúde (Health Checks)
- **API**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **MySQL**: `docker exec -it graham-select-mysql mysqladmin ping -h localhost -uroot -proot`
- **Frontend**: [http://localhost:3000](http://localhost:3000)

