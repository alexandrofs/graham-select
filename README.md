# Graham Select

**Graham Select** é um aplicativo especializado em identificar as 20 empresas mais baratas da bolsa utilizando a metodologia de valor de **Benjamin Graham** (Fórmula do Valor Intrínseco).

O sistema automatiza o processamento de indicadores financeiros para determinar quais empresas estão subvalorizadas, fornecendo uma margem de segurança clara para o investidor.

## 🚀 Visão Geral e Arquitetura

O projeto utiliza uma arquitetura de microserviços assíncronos para garantir escalabilidade e separação de responsabilidades.

- **Frontend (Flutter Web)**: Interface moderna e interativa para upload de dados e visualização do ranking.
- **API Service (Spring Boot)**: Ponto de entrada que gerencia o upload de arquivos e fornece os resultados processados.
- **Valuation Service (Spring Boot)**: Motor de cálculo que consome eventos de dados financeiros e aplica a regra de Graham.
- **Apache Kafka**: Broker de mensagens que desacopla o recebimento de dados (API) do processamento pesado (Valuation).
- **MySQL**: Persistência de dados das empresas e resultados dos cálculos.

[Mais detalhes na Visão do Produto](docs/visao-produto.md)

---

## 🛠️ Tecnologias Utilizadas

- **Linguagens**: Dart (Flutter), Java 21
- **Frameworks**: Spring Boot 3, Flutter
- **Mensageria**: Apache Kafka
- **Banco de Dados**: MySQL
- **DevOps**: Docker, Buildpacks (Spring Native), GitHub Actions (CI/CD)

---

## 💻 Execução Local

A forma mais simples de rodar todo o ecossistema (Infra + Backend + Frontend) é utilizando o script automatizado:

### Pré-requisitos
- **Docker Desktop** instalado e rodando.
- **Java 21** e **Flutter SDK** instalados.

### Passo a Passo
1. **Clone o Repositório**
   ```bash
   git clone https://github.com/alexandrofs/graham-select.git
   cd graham-select
   ```

2. **Configuração do Google Sign-In (Login Web)**
   O login com o Google no modo web exige a criação de um **Client ID** no seu projeto do Google Cloud e a habilitação da **People API**.
   * [Acesse o Google Cloud Console](https://console.cloud.google.com/apis/credentials).
   * Crie ou selecione um projeto.
   * Vá em **APIs & Services > Library**, pesquise por **Google People API** e clique em **Enable** (necessário para buscar o perfil do usuário).
   * Retorne para **APIs & Services > Credentials** e clique em **Create Credentials > OAuth client ID**.
   * Em Application Type, escolha **Web application**.
   * Em *Authorized JavaScript origins*, adicione a URL local (ex: `http://localhost:3000`).
   * Em *Authorized redirect URIs*, você pode deixar em branco ou preencher com a mesma URL.
   * Clique em **Create**.
   * Copie o valor gerado (Exemplo: `seu-id.apps.googleusercontent.com`).

3. **Configuração do Firebase Auth**
   Para que a troca do token do Google para o Firebase aconteça, o projeto Firebase precisa estar vinculado ao mesmo Google Cloud criado acima.
   * Acesse o [Firebase Console](https://console.firebase.google.com/).
   * Clique em **Add project** e selecione na lista o mesmo projeto do Google Cloud que você acabou de usar.
   * Vá em **Build > Authentication > Sign-in method** e habilite o provedor **Google** (adicione o e-mail de suporte solicitado).
   * Na página inicial (*Project Overview*), adicione um app **Web** ao projeto (ícone `</>`).
   * No final da criação do app, o Firebase fornecerá um snippet de código contendo o `firebaseConfig`. Copie os valores do `apiKey`, `appId`, `messagingSenderId` e `projectId`.

4. **Execute o Inicializador**
   Para testar o *Google Sign-In* no frontend web, defina as variáveis relacionadas ao GCP e Firebase (obtidas nos passos anteriores) antes de executar o script.
   
   ```bash
   export GOOGLE_CLIENT_ID="seu-client-id.apps.googleusercontent.com"
   export FIREBASE_API_KEY="AIzaSy..."
   export FIREBASE_AUTH_DOMAIN="seu-projeto.firebaseapp.com"
   export FIREBASE_PROJECT_ID="seu-projeto"
   export FIREBASE_STORAGE_BUCKET="seu-projeto.firebasestorage.app"
   export FIREBASE_MESSAGING_SENDER_ID="12345"
   export FIREBASE_APP_ID="1:12345:web:abcde"
   export FIREBASE_MEASUREMENT_ID="G-12345"
   
   ./start-app.sh
   ```
   *Este script irá:*
   - Subir o MySQL e Kafka via Docker Compose.
   - Compilar o módulo `common`.
   - Iniciar a **API** na porta `8080`.
   - Iniciar o **Valuation Service** na porta `8081`.
   - Iniciar o **Frontend Web** na porta `3000`.

### Acesso aos Serviços
- **Frontend Web**: [http://localhost:3000](http://localhost:3000)
- **API Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **CORS**: Configurado para aceitar requisições de `localhost:3000`.

---

## 📦 Builds de Produção (Buildpacks)

O projeto está configurado para gerar imagens Docker nativas via Cloud Native Buildpacks:

```bash
# No diretório backend
./mvnw spring-boot:build-image -pl api
./mvnw spring-boot:build-image -pl valuation-service
```

---

## 🚀 Deploy em Produção (PaaS Gratuito)

O sistema possui uma esteira contínua (CD) preparada para provedores serverless Cloud-Native.

- **Serviços de Computação (Frontend, API e Valuation)**: [Render.com](https://render.com)
- **Banco de Dados**: [TiDB Serverless](https://tidbcloud.com)
- **Mensageria (Kafka)**: [Aiven](https://aiven.io)

[👉 Veja o Guia Completo de Infraestrutura e Automação (Render, TiDB, Aiven)](docs/deploy.md)

---

## 📖 Documentação da API

Os principais endpoints estão disponíveis via API REST:

| Endpoint | Método | Descrição |
| :--- | :--- | :--- |
| `/api/v1/upload-financial-data` | `POST` | Faz o upload do arquivo CSV/XLS para processamento. |
| `/api/v1/ranked-companies` | `GET` | Retorna o ranking das 20 empresas mais baratas. |

---

## 🤝 Contribuindo

1. Faça um **fork** do repositório.
2. Crie uma branch (`git checkout -b feature/minha-feature`).
3. Commit suas mudanças (`git commit -m 'feat: minha nova feature'`).
4. Push para a branch (`git push origin feature/minha-feature`).
5. Abra um **Pull Request**.

---

## 📄 Licença e Contato

- **Licença**: Apache 2.0
- **Contato**: afssistemas@gmail.com
