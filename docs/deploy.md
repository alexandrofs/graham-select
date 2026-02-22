# Implantação em Produção (PaaS Gratuito)

Este documento descreve como realizar o deploy do **Graham Select** utilizando uma arquitetura Cloud-Native desacoplada, utilizando serviços que oferecem tiers gratuitos generosos (Serverless / PaaS).

## Arquitetura de Produção

- **Frontend**: Vercel
- **Backend (API e Valuation)**: Render.com
- **Banco de Dados**: TiDB Serverless (Compatível com MySQL)
- **Mensageria**: Upstash Kafka

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

## 2. Mensageria (Upstash Kafka)

O Upstash fornece Kafka serverless gratuito (até 10.000 msgs/dia).

1. Crie uma conta em [Upstash](https://upstash.com/).
2. Vá em **Kafka** e crie um novo Cluster (ex: na região US-East).
3. Após criar, role para a seção **REST API** ou **Details**. Você precisará das seguintes informações:
   - **Endpoint** (Bootstrap Server)
   - **Username**
   - **Password**
4. Você precisa formatar a string JAAS para o Spring Boot:
   `org.apache.kafka.common.security.scram.ScramLoginModule required username="SEU_USER" password="SUE_PASSWORD";`

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

### Passo 3.3: Configurar CD no GitHub Actions
Para que o sistema atualize o backend automaticamente ao dar push na `main`:

1. Vá no seu repositório no GitHub > **Settings** > **Secrets and variables** > **Actions**.
2. Crie a secret `RENDER_API_DEPLOY_HOOK` e cole a URL do Hook da API.
3. Crie a secret `RENDER_VALUATION_DEPLOY_HOOK` e cole a URL do Hook do Valuation.

A pipeline `.github/workflows/deploy-backend.yml` agora chamará esses hooks automaticamente!

## 4. Frontend (Vercel)

A Vercel hospedará a o Frontend em Flutter Web de forma muito simples.

1. Crie uma conta na [Vercel](https://vercel.com) associada ao seu GitHub.
2. Clique em **Add New Project** e importe o repositório `graham-select`.
3. Na seção **Build and Output Settings**:
   - Framework Preset: **Other**
   - Build Command: `flutter pub get && flutter build web --release`
   - Output Directory: `build/web`
   - Install Command: `git clone https://github.com/flutter/flutter.git -b stable ../flutter && export PATH="$PATH:`pwd`/../flutter/bin" && flutter doctor` (Este é um truque para a Vercel baixar o Flutter no build).
4. Em **Environment Variables**, configure:
   - `API_BASE_URL`: A URL pública da sua API no Render (ex: `https://graham-select-api.onrender.com`). *Nota: Certifique-se de que o frontend Flutter está lendo essa variável no código (via `--dart-define` ou arquivo de config).*
5. Clique em **Deploy**.

> Toda vez que você fizer push na `main`, a Vercel iniciará um novo build e fará o deploy automático.
