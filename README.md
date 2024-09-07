# Graham Select

**Graham Select** é um aplicativo para identificar as 20 empresas mais baratas usando a regra de Benjamin Graham. O sistema realiza a análise de dados financeiros para determinar quais empresas estão subvalorizadas, proporcionando insights valiosos para investidores.

## Visão Geral

O **Graham Select** é composto por diversos serviços que trabalham juntos para processar e analisar dados financeiros. O sistema utiliza uma arquitetura baseada em microserviços, com um frontend interativo, uma API para processamento de dados, e serviços especializados para ingestão e análise de dados financeiros.

### Funcionalidades

- **Frontend (Flutter)**: Interface gráfica para interação do usuário.
- **API (Spring Boot)**: Exposição de APIs para consulta de dados e upload de arquivos.
- **Upload Service**: Serviço para upload de arquivos e ingestão de dados no Kafka.
- **Data Ingestion Service**: Consome dados do Kafka e os armazena no banco de dados.
- **Calculation Service**: Calcula o valor intrínseco das empresas e armazena os resultados.
- **Kafka**: Sistema de mensageria para comunicação entre serviços.
- **Database (MySQL)**: Armazena dados financeiros e resultados dos cálculos.

[Mais detalhes](docs/visao-produto.md)

## Arquitetura

[Diagramas C4 Model](https://alexandrofs.github.io/graham-select)

O diagrama acima mostra a arquitetura geral do **Graham Select**, incluindo os principais componentes e suas interações.

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
   git clone https://github.com/alexandrofs/graham-select.git
   cd graham-select
   ```
2. **Configuração do Frontend**

Navegue até o diretório do frontend e instale as dependências:

   ```bash
   cd frontend
   flutter pub get
   flutter run
   ```
3. **Configuração da API**

Navegue até o diretório da API e execute:

   ```bash
   cd api
   ./mvnw spring-boot:run
   ```
4. **Configuração do Kafka**

   Siga as instruções na documentação do Kafka para configurar o Kafka localmente.

5. **Configuração do Banco de Dados**

   Certifique-se de que o MySQL esteja em execução e configure o banco de dados conforme necessário.

## Contribuindo

Contribuições são bem-vindas! Siga os seguintes passos para contribuir:

1. Faça um fork do repositório.
2. Crie uma branch para suas modificações (git checkout -b feature/nova-funcionalidade).
3. Faça commit das suas alterações (git commit -am 'Adiciona nova funcionalidade').
4. Faça um push para a branch (git push origin feature/nova-funcionalidade).
5. Abra um Pull Request.

## Licença

Este projeto está licenciado sob a Apache 2.0 License.

## Contato

Para perguntas ou sugestões, entre em contato com afssistemas@gmail.com.
