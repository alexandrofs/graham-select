# Implantação em Produção (PaaS Gratuito)

Este documento descreve como realizar o deploy do **Graham Select** utilizando uma arquitetura Cloud-Native desacoplada, centralizando nossos serviços na plataforma Render e containers GHCR.

## Arquitetura de Produção Consolidada

- **Serviços de Computação (Frontend, API e Valuation)**: Render.com
- **Banco de Dados**: TiDB Serverless (Compatível com MySQL)
- **Mensageria**: Aiven for Apache Kafka

---

## 1. Banco de Dados (TiDB Serverless)

O TiDB oferece um cluster serverless gratuito compatível com MySQL.

1. Crie uma conta em [TiDB Cloud](https://tidbcloud.com/).
2. Crie um novo cluster Serverless.
3. No painel de Overview, clique em **Connect** e escolha **Connect With... "General"**.
4. Copie os dados:
   - Host (ex: `gateway01.us-east-1.prod.aws.tidbcloud.com`)
   - Port (ex: `4000`)
   - User (ex: `xxxxx.root`)
   - Password

> **Nota:** O banco de dados se chama `test` por padrão. Você pode usá-lo ou criar o `grahamselect`. O JPA/Hibernate na API criará as tabelas automaticamente.

## 2. Mensageria (Aiven for Apache Kafka)

A plataforma [Aiven](https://aiven.io) fornece um "Free Plan" excelente para desenvolvedores que inclui um cluster de Apache Kafka gratuito.

1. Crie uma conta no consolo da [Aiven](https://console.aiven.io/).
2. Clique em **Create service**, selecione **Apache Kafka**.
3. Escolha o cloud provider e a região da sua preferência. Selecione o plano **Free** e clique em criar (pode levar alguns minutos).
4. Vá para a aba **Overview** do seu serviço recém-criado.
5. Copie o **Service URI**. Ele já vem no formato:
   `kafka://<usuario>:<senha>@<host>:<porta>`
6. Com essa URI, você extrai o **Bootstrap Server**, **Username** e **Password**.
7. Formate a string JAAS para o Spring Boot:
   `org.apache.kafka.common.security.plain.PlainLoginModule required username="SEU_USER" password="SUE_PASSWORD";`

## 3. Backend (Render.com)

O Render permite rodar containers Docker a partir do GHCR. Precisamos criar dois **Web Services**: um para a API e outro para o Valuation.

### Requisito Prévio: Acesso ao GHCR
Como nossas imagens no GHCR (`ghcr.io/alexandrofs/graham-select-api` e `valuation`) são públicas, o Render consegue puxar diretamente.

### Passo 3.1: Deploy da API
1. No Render, crie um novo **Web Service**.
2. Escolha a opção **Deploy an existing image from a registry**.
3. Image URL: `ghcr.io/alexandrofs/graham-select-api:latest`
4. Selecione o plano **Free**.
5. Em **Environment Variables**, adicione (mesclando TiDB e Upstash):
   - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<TIDB_HOST>:<TIDB_PORT>/<DATABASE>?sslMode=VERIFY_IDENTITY`
   - `SPRING_DATASOURCE_USERNAME`: `<TIDB_USER>`
   - `SPRING_DATASOURCE_PASSWORD`: `<TIDB_PASSWORD>`
   - `KAFKA_BOOTSTRAP_SERVERS`: `<UPSTASH_ENDPOINT>`
   - `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL`: `SASL_SSL`
   - `SPRING_KAFKA_PROPERTIES_SASL_MECHANISM`: `SCRAM-SHA-256`
   - `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG`: `org.apache.kafka.common.security.scram.ScramLoginModule required username="..." password="...";`
6. Salve e faça o deploy.
7. Após o deploy, vá na aba **Settings** do serviço no Render, role até **Deploy Hook** e copie a URL (ex: `https://api.render.com/deploy/srv-...`).

### Passo 3.2: Deploy do Valuation Service
1. Repita o processo criando outro **Web Service**.
2. Image URL: `ghcr.io/alexandrofs/graham-select-valuation:latest`
3. Copie exatamente as mesmas variáveis de ambiente do passo 3.1.
4. Salve e faça o deploy.
5. Vá em **Settings** -> **Deploy Hook** e copie a URL.

### Passo 3.3: Deploy do Frontend (Web)
1. Crie um novo **Web Service**.
2. Escolha **Deploy an existing image from a registry**.
3. Image URL: `ghcr.io/alexandrofs/graham-select-frontend:latest`
4. Selecione o plano **Free**.
5. Em **Environment Variables**, adicione a URL base da sua API:
   - `API_BASE_URL`: `https://sua-api.onrender.com` (Nota: Garanta que o App Flutter consiga ler essa ENV).
6. Salve e faça o deploy. O Frontend rodará em um mini-servidor Nginx otimizado.
7. Vá em **Settings** -> **Deploy Hook** e copie a URL.

### Passo 3.4: Configurar Auto-Deploy via GitHub (CD)
Para que as atualizações cheguem no Render automaticamente a cada push na `main`:

1. No repositório GitHub, vá em **Settings** > **Secrets and variables** > **Actions**.
2. Adicione as 3 URLs de Deployment Hooks geradas no passo anterior como secrets:
   - `RENDER_API_DEPLOY_HOOK`: URL do hook da API
   - `RENDER_VALUATION_DEPLOY_HOOK`: URL do hook do Valuation
   - `RENDER_FRONTEND_DEPLOY_HOOK`: URL do hook do Frontend

Pronto! Os scripts do GitHub Actions chamarão esses hooks via script na nuvem de forma orgânica.
