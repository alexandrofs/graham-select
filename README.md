# GrahamSelect

**GrahamSelect** é um aplicativo para identificar as 10 empresas mais baratas usando a regra de Benjamin Graham. O sistema realiza a análise de dados financeiros para determinar quais empresas estão subvalorizadas, proporcionando insights valiosos para investidores.

## Visão Geral

O **GrahamSelect** é composto por diversos serviços que trabalham juntos para processar e analisar dados financeiros. O sistema utiliza uma arquitetura baseada em microserviços, com um frontend interativo, uma API para processamento de dados, e serviços especializados para ingestão e análise de dados financeiros.

### Funcionalidades

- **Frontend (Flutter)**: Interface gráfica para interação do usuário.
- **API (Spring Boot)**: Exposição de APIs para consulta de dados e upload de arquivos.
- **Upload Service**: Serviço para upload de arquivos e ingestão de dados no Kafka.
- **Data Ingestion Service**: Consome dados do Kafka e os armazena no banco de dados.
- **Calculation Service**: Calcula o valor intrínseco das empresas e armazena os resultados.
- **Kafka**: Sistema de mensageria para comunicação entre serviços.
- **Database (MySQL)**: Armazena dados financeiros e resultados dos cálculos.

## Arquitetura

![Diagrama C4](caminho/para/seu/diagrama.png)

O diagrama acima mostra a arquitetura geral do **GrahamSelect**, incluindo os principais componentes e suas interações.

## Tecnologias Utilizadas

- **Frontend**: Flutter
- **Backend**: Java, Spring Boot
- **Mensageria**: Apache Kafka
- **Banco de Dados**: MySQL
- **Orquestração**: Kubernetes

## Instalação e Configuração

### Pré-requisitos

- Java 21 ou superior
- Node.js e npm (para Flutter)
- Docker e Kubernetes (para implantação)

### Passos para Execução Local

1. **Clone o Repositório**

   ```bash
   git clone https://github.com/SEU_USUARIO/GrahamSelect.git
   cd GrahamSelect
