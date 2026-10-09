# CLYVO PAWS (CLYVO VET) - FIAP CHALLENGE 2026

API RESTful desenvolvida para o sistema de gerenciamento veterinário Clyvo Paws.
Este projeto visa digitalizar e otimizar o atendimento clínico, histórico médico
e acompanhamento preventivo de pets, servindo de backend para o app mobile
(React Native/Expo) desenvolvido em paralelo pela mesma equipe.
 
--------------------------------------------------------------------------------
## 🔗 API EM PRODUÇÃO (DEPLOY)
--------------------------------------------------------------------------------
A API está publicada no Render e pode ser testada sem instalar nada:

* **Swagger UI:** https://clyvopawsjavachallenge.onrender.com/swagger-ui.html
  ⚠️ **Primeira requisição lenta:** a hospedagem usa o plano gratuito do Render,
  que "dorme" o serviço após ~15 minutos sem uso. A primeira chamada depois
  disso pode levar cerca de 1 minuto para responder. Abra o Swagger e aguarde
  carregar antes de testar os endpoints.

ℹ️ A raiz da URL (`/`) responde **401** porque não é uma rota pública (toda
rota fora do Swagger e do login exige token). Use o link do Swagger acima.

**Como testar rapidamente:**
1. Abra o Swagger e faça `POST /login` com o usuário de teste (ver "Usuários de
   teste" abaixo).
2. Copie o `token` da resposta, clique em **Authorize** (cadeado) e cole o
   token puro (sem escrever "Bearer").
3. Chame qualquer endpoint pelo "Try it out".
--------------------------------------------------------------------------------
## 👥 INTEGRANTES DO GRUPO (Turma: 2TDSPX)
--------------------------------------------------------------------------------
* Felipe Ribeiro Salles de Camargo | RM: 565224
* João Pedro Pereira Camilo        | RM: 562005
* Lucas Matsubara Reis             | RM: 565020
* Pamella Christiny Chaves Brito   | RM: 565206
--------------------------------------------------------------------------------
## 🧱 STACK TÉCNICA
--------------------------------------------------------------------------------
* **Linguagem**: Java 21
* **Framework**: Spring Boot 3.2.6
* **Persistência**: Spring Data JPA + Hibernate, banco Oracle
* **Migrations**: Flyway (`flyway-core` + `flyway-database-oracle`)
* **Segurança**: Spring Security + OAuth2 Resource Server, JWT assinado com
  par de chaves RSA (Nimbus JOSE+JWT)
* **Validação**: Bean Validation (Jakarta Validation / Hibernate Validator)
* **Documentação**: springdoc-openapi (Swagger UI)
* **Build**: Maven
* **Deploy**: Docker (build multi-stage) hospedado no Render
* **Utilitários**: Lombok
--------------------------------------------------------------------------------
## 🚀 ATENDIMENTO AOS REQUISITOS (JAVA ADVANCED)
--------------------------------------------------------------------------------
Para facilitar a correção pelo professor, abaixo estão os pontos chave exigidos
no escopo do Challenge:

### Spring Security (autenticação e autorização)
1. Autenticação stateless via JWT, assinado com par de chaves RSA (Spring
   Security OAuth2 Resource Server + Nimbus JOSE+JWT). Senhas armazenadas com
   BCrypt.
2. Três perfis de usuário com permissões diferentes: TUTOR, VETERINARIO e
   ADMIN — ver tabela completa em "Regras de Autorização" abaixo.
3. Proteção de rotas por perfil (`hasRole`) **e** por posse do recurso (ex:
   um tutor só acessa/edita os próprios pets, consultas e agendamentos; um
   veterinário só vê/gerencia as próprias consultas), reforçada dentro dos
   Services via `AuthorizationService`.
4. Credenciais do banco e chaves RSA **fora do código-fonte**: usuário e senha
   do Oracle vêm de variáveis de ambiente (`DB_USERNAME`, `DB_PASSWORD`) e as
   chaves RSA não são versionadas (`.gitignore`).
### Flyway (controle de versão do banco)
5. Migrations versionadas (`V1`, `V2`, `V3`) com criação de estrutura, carga
   inicial de dados de teste e seed do usuário administrativo.
### Funcionalidades completas (fluxos além de simples CRUD)
6. Fluxo de agendamento de consulta: o tutor escolhe pet, clínica, veterinário
   e horário; `POST /agendamentos` aceita tanto reaproveitar uma Consulta já
   existente (`consultaId`) quanto criar a Consulta na hora a partir de
   `petId` + `clinicaId` + `veterinarioId`, checando a disponibilidade de
   horário na agenda do veterinário antes de reservar.
7. Fluxo de acompanhamento de medicação: registro de cada dose tomada pelo
   pet (`POST /medicamentos/doses/check`), com validação de que o registro
   não pode ser feito com data futura.
### Demais boas práticas
8. Validação de campos (Bean Validation): implementado via DTOs (`@NotBlank`,
   `@Size` alinhado aos limites reais das colunas no banco, `@Email`,
   `@Pattern` para CPF/CNPJ/CEP, `@Positive`, `@PastOrPresent`, `@Future`),
   incluindo validação em cascata de objetos aninhados (`@Valid`).
9. Paginação e Ordenação: implementado via `@PageableDefault` e
   `@ParameterObject` nos endpoints de listagem (Tutores, Pets, Veterinários,
   Consultas, Medicamentos, Agendamentos, Clínicas, Catálogo Preventivo).
10. Busca com parâmetros: implementado em `GET /planos-preventivos/busca?especie=CACHORRO`.
11. Uso de Cache: implementado com `@EnableCaching`, `@Cacheable` e
    `@CacheEvict` na Service do Catálogo Preventivo (cache invalidado
    sempre que um plano é criado, alterado ou excluído).
12. Tratamento de Exceções: implementado via `@RestControllerAdvice`
    (`GlobalExceptionHandler`), mapeando erros de validação e regra de
    negócio (400), credenciais inválidas (401), acesso negado (403),
    recurso não encontrado (404) e parâmetro de ordenação inválido (400),
    além de um handler genérico para qualquer falha não mapeada (500), que
    registra o stack trace no log.
13. Relacionamentos JPA e DTOs: implementado Data Shaping (Slim Payloads)
    para otimizar as consultas evitando loops infinitos, com cascata de
    exclusão configurada corretamente em toda a árvore de relacionamentos
    (Tutor → Pet → Consulta → Medicamento/Agendamento) para evitar erro de
    violação de chave estrangeira ao excluir um registro pai.
--------------------------------------------------------------------------------
## 🛡️ REGRAS DE AUTORIZAÇÃO (quem pode fazer o quê)
--------------------------------------------------------------------------------
Além do perfil (TUTOR / VETERINARIO / ADMIN) checado nas rotas, o
`AuthorizationService` (pacote `auth`) reforça a posse do registro dentro
dos Services de domínio:

| Recurso                        | Regra |
|---------------------------------|-------|
| Tutor (perfil)                  | Tutor só vê/edita/exclui o próprio; VETERINARIO/ADMIN veem qualquer um |
| Pet                             | Tutor só cria/vê/edita/exclui os próprios pets; VETERINARIO/ADMIN veem todos |
| Consulta                        | VETERINARIO só vê/gerencia as consultas em que é o responsável; tutor só vê consultas dos próprios pets (somente leitura); ADMIN acesso total |
| Medicamento / registro de dose  | Tutor vê e registra dose só nos próprios pets; veterinário só nas consultas em que é responsável; ADMIN acesso total |
| Agendamento                     | Tutor só cria/edita/exclui agendamentos ligados aos próprios pets. Consulta por id ou por consulta: tutor dono, veterinário responsável ou ADMIN. Listagem por pet (`/agendamentos/pet/{id}`) e por tutor (`/agendamentos/tutor/{id}`): só o tutor dono ou ADMIN. Listagem geral: só ADMIN |
| AgendaDisponivel (horários livres do vet) | Criar/excluir: só o próprio veterinário. Ver horários livres: qualquer autenticado (tutor navegando pra agendar, ou o próprio vet) |
| Veterinario (perfil)            | Cadastro: só ADMIN. Listagem/detalhe: qualquer autenticado. Editar/excluir: só o próprio vet ou ADMIN |
| Clinica / CatalogoPreventivo    | Leitura: qualquer autenticado. Escrita (POST/PUT/DELETE): só ADMIN |

Sem token ou com credenciais inválidas a API responde **401**; tentativas
fora dessas regras (token válido, mas sem permissão sobre o recurso)
retornam **403 Forbidden**.
 
--------------------------------------------------------------------------------
## 🌐 PRINCIPAIS ENDPOINTS
--------------------------------------------------------------------------------
A lista completa e interativa está no Swagger (veja a seção de execução
abaixo). Resumo dos grupos de rotas:

| Grupo | Endpoints principais |
|---|---|
| Autenticação | `POST /login` |
| Tutores | `POST /tutores` (público), `GET/PUT/DELETE /tutores/{id}`, `GET /tutores` |
| Pets | `POST /pets`, `GET/PUT/DELETE /pets/{id}`, `GET /pets/tutor/{tutorId}`, `GET /pets` |
| Veterinários | `POST /veterinarios` (só ADMIN), `GET/PUT/DELETE /veterinarios/{id}`, `GET /veterinarios` |
| Clínicas | `GET /clinicas`, `GET /clinicas/{id}`, `POST/PUT/DELETE /clinicas/{id}` (só ADMIN) |
| Consultas | `POST /consultas`, `GET/PUT/DELETE /consultas/{id}`, `GET /consultas/pet/{petId}`, `GET /consultas` |
| Medicamentos | `POST /medicamentos`, `GET/PUT/DELETE /medicamentos/{id}`, `GET /medicamentos/consulta/{consultaId}`, `POST /medicamentos/doses/check`, `GET /medicamentos` |
| Agendamentos | `POST /agendamentos` (com ou sem `consultaId` — sem ele, cria a consulta na hora), `GET/PUT/DELETE /agendamentos/{id}`, `GET /agendamentos/consulta/{consultaId}`, `GET /agendamentos/pet/{petId}`, `GET /agendamentos/tutor/{tutorId}` |
| Agenda do veterinário | `POST /agendas` (só o próprio vet), `GET /agendas/veterinario/{veterinarioId}`, `DELETE /agendas/{id}` |
| Catálogo preventivo | `GET /planos-preventivos`, `GET /planos-preventivos/busca?especie=CACHORRO`, `POST/PUT/DELETE /planos-preventivos/{id}` (só ADMIN) |

💡 **Dica de uso nos endpoints paginados:** o Swagger mostra `page`, `size` e
`sort` como campos separados. Deixe `sort` **vazio** para usar a ordenação
padrão, ou informe um campo real da entidade (ex: `dataHora,DESC`). Um campo
inexistente (como o texto `string`) retorna **400** com mensagem explicativa.
 
--------------------------------------------------------------------------------
## ⚠️ LIMITAÇÕES CONHECIDAS
--------------------------------------------------------------------------------
* `PUT /tutores/{id}` (via `TutorUpdateDTO`) atualiza nome, e-mail, CPF,
  telefone e foto, mas **não atualiza o endereço** do tutor. Essa decisão foi
  tomada porque, na aplicação mobile, a localização é solicitada ao tutor
  diretamente pelo app (com a permissão do usuário).
* Ao alterar o e-mail de um tutor, o `username` de login (`tb_user`) é
  sincronizado automaticamente — mas o token JWT já emitido continua válido
  com o e-mail **antigo** como identidade até expirar. Depois de trocar o
  e-mail, é necessário fazer login novamente para obter um token atualizado.
* Somente o login de **TUTOR** devolve o próprio perfil (`nomeCompleto`,
  `fotoUrl`, `id`) direto na resposta de `POST /login`. Um VETERINARIO ou
  ADMIN autenticado recebe só o `token`; ainda não há endpoint dedicado para
  esses perfis descobrirem o próprio ID depois do login.
* Veterinários não têm acesso às rotas `GET /agendamentos/pet/{id}` e
  `GET /agendamentos/tutor/{id}` (restritas ao tutor dono e ao ADMIN); eles
  consultam seus agendamentos por consulta (`GET /agendamentos/consulta/{id}`).
* Na hospedagem gratuita, o serviço dorme após inatividade (ver seção
  "API em Produção").
--------------------------------------------------------------------------------
## ☁️ DEPLOY (RENDER + DOCKER)
--------------------------------------------------------------------------------
A aplicação é publicada no Render como **Web Service com Docker**, usando o
`Dockerfile` da raiz (build multi-stage: Maven + JDK 21 para compilar,
imagem JRE 21 para executar). O banco continua sendo o Oracle da FIAP.

Nada sensível fica no repositório. A configuração de produção é feita no
painel do Render (aba **Environment**):

| Variável | Valor |
|---|---|
| `DB_USERNAME` | usuário do Oracle |
| `DB_PASSWORD` | senha do Oracle |
| `RSA_PRIVATE_KEY` | `file:/etc/secrets/private_key.pem` |
| `RSA_PUBLIC_KEY` | `file:/etc/secrets/public_key.pem` |
| `JAVA_TOOL_OPTIONS` | `-Xmx384m` (limita a memória da JVM no plano gratuito) |

E, em **Secret Files**, dois arquivos com o conteúdo das chaves RSA de
produção: `private_key.pem` e `public_key.pem` (montados em `/etc/secrets/`).

O `application.properties` lê tudo isso por placeholders
(`${DB_USERNAME}`, `${DB_PASSWORD}`, `${PORT:8080}`) e as propriedades
`rsa.private-key` / `rsa.public-key` são sobrescritas pelas variáveis
`RSA_PRIVATE_KEY` / `RSA_PUBLIC_KEY`. O Render define a porta pela variável
`PORT`, e `server.forward-headers-strategy=framework` faz o Swagger gerar
links `https` corretos atrás do proxy.
 
--------------------------------------------------------------------------------
## ⚙️ COMO EXECUTAR O PROJETO LOCALMENTE (PASSO A PASSO)
--------------------------------------------------------------------------------
> Para apenas testar a API, use a versão publicada (seção "API em Produção").
> Rodar localmente só é necessário para desenvolvimento.

### Pré-requisitos
* JDK 21
* Maven
* Acesso a um banco Oracle (ex: o Oracle da FIAP) — usuário e senha
* OpenSSL (para gerar as chaves RSA — já vem instalado em Linux/Mac; no
  Windows costuma vir junto com o Git Bash)
### Passo 1 — Clonar e informar as credenciais do banco
1. Clone o repositório e abra na sua IDE.
2. As credenciais **não ficam no código**. Defina as variáveis de ambiente
   antes de subir a aplicação:
    * `DB_USERNAME` — usuário do Oracle
    * `DB_PASSWORD` — senha do Oracle
    * (opcional) `DB_URL` — URL JDBC; se omitida, usa a do Oracle da FIAP
      (`jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL`)
      No IntelliJ: *Run → Edit Configurations → Environment variables* →
      `DB_USERNAME=seu_usuario;DB_PASSWORD=sua_senha`.
      No terminal (Linux/Mac): `export DB_USERNAME=... DB_PASSWORD=...`.
      No Windows (cmd): `set DB_USERNAME=...` e `set DB_PASSWORD=...`.
### Passo 2 — Gerar o par de chaves RSA (obrigatório)
A API usa JWT assinado com RSA. As chaves NÃO ficam versionadas no Git
(`.gitignore`) — cada máquina/clone precisa gerar o próprio par antes do
primeiro run:
```
mkdir -p src/main/resources/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/keys/private_key.pem
openssl rsa -pubout -in src/main/resources/keys/private_key.pem -out src/main/resources/keys/public_key.pem
```
Sem isso, a aplicação falha ao subir (bean do `JwtDecoder`/`JwtEncoder` não
consegue ser criado).

### Passo 3 — Subir a aplicação
Os dados de teste (`V1`, `V2`, `V3`) já vêm com hashes de senha reais
prontos — não é preciso gerar nada além das chaves do Passo 2. Só rodar:
```
mvn spring-boot:run
```
Ou execute a classe `ClyvovetApplication` direto pela IDE. O Flyway aplica
as migrations automaticamente e o servidor sobe em `http://localhost:8080`.

⚠️ Se em algum momento você editar uma migration do Flyway (`V1`/`V2`/`V3`)
depois que ela já foi aplicada num banco, a próxima subida quebra com erro
de checksum ("Migration checksum mismatch"). Se isso acontecer, apague as
tabelas e a `flyway_schema_history` do seu schema e suba de novo.

### Passo 4 — Conferir que subiu
Acesse `http://localhost:8080/swagger-ui.html` — deve listar todos os
endpoints, com um botão verde "Authorize" no topo da página.

### Usuários de teste (dados do seed)
* Tutores/Veterinários: `<username do seed, ex. tutor1@email.com>` / `Senha@123`
* Admin (único perfil que pode cadastrar veterinário via `POST /veterinarios`):
  `admin@clyvopaws.com` / `Admin@123`
  ⚠️ No login, informe a **senha em texto** (ex: `Admin@123`), nunca o hash
  bcrypt que aparece nas migrations.

--------------------------------------------------------------------------------
## 🔑 AUTENTICAÇÃO: COMO USAR O TOKEN JWT
--------------------------------------------------------------------------------
Faça `POST /login` com `{"username": "...", "password": "..."}`. A resposta
traz:
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "nomeCompleto": "Ana Souza",
  "fotoUrl": "https://...",
  "id": 3
}
```
`nomeCompleto`, `fotoUrl` e `id` só vêm preenchidos quando quem faz login é
um **TUTOR** (é o próprio ID do tutor, útil pra montar as demais chamadas
como `GET /pets/tutor/{tutorId}` sem precisar de outra requisição). Pra
VETERINARIO/ADMIN, esses campos vêm nulos ou com o `username` no lugar do
nome — só o `token` é garantido pra qualquer perfil. O token expira em 30
minutos; credenciais inválidas retornam **401**.

O token vai no header `Authorization: Bearer <token>` em toda rota protegida.

**No Swagger UI:**
1. Faça `POST /login` pelo "Try it out" e copie o valor de `token` da resposta.
2. Clique no botão verde "Authorize" (cadeado) no topo da página.
3. Cole o token puro no campo `bearerAuth` (sem o prefixo "Bearer", o Swagger
   adiciona sozinho) e clique em "Authorize".
4. Toda chamada feita pelo "Try it out" a partir daí já sai autenticada.
   **No Postman:**
1. Faça o `POST /login` numa requisição normal e copie o `token` da resposta.
2. Na requisição que quer autenticar, aba "Authorization" → Type "Bearer Token"
   → cole o token puro no campo "Token".
3. (Opcional) Automatize com uma variável de collection: no `POST /login`, aba
   "Tests", adicione `pm.collectionVariables.set('token', pm.response.json().token);`
   e use `{{token}}` como Bearer Token nas demais requisições.
--------------------------------------------------------------------------------
## 📱 CONECTANDO COM O APP MOBILE (React Native / Expo Go)
--------------------------------------------------------------------------------
Com a API publicada, o app pode apontar direto para a URL do Render
(`https://clyvopawsjavachallenge.onrender.com`), sem depender de IP local.

O CORS (`CorsConfig.java`) está liberado para `http://localhost:8081` nos
métodos GET/POST/PUT/DELETE/PATCH/OPTIONS. Isso só importa se o app for
testado via **Expo Web** (navegador) — testando via **Expo Go** nativo
(celular físico ou emulador), CORS não se aplica, então não é preciso mexer
nisso.

Para testar contra a API rodando **localmente** via **Expo Go num celular
físico**, na mesma rede Wi-Fi do PC onde a API roda:

1. `localhost` no celular aponta pro próprio celular, não pro PC. No código
   do app RN, use o IP local do PC (Windows: `ipconfig` → endereço IPv4 do
   adaptador Wi-Fi, ex: `http://192.168.0.105:8080`), nunca `localhost:8080`.
2. O Firewall do Windows pode bloquear a conexão vinda do celular na porta
   8080 — libere o Java (ou a porta) pra redes "Privada" em Firewall do
   Windows Defender → Permitir um aplicativo.
3. Teste rápido pra isolar o problema: abra
   `http://<IP-DO-PC>:8080/swagger-ui.html` no navegador do próprio celular.
   Se abrir, a rede está OK e qualquer erro daí em diante é no app, não na
   conexão.
   Testando via **emulador Android**: use `http://10.0.2.2:8080` (alias
   especial que o emulador usa pra apontar pro `localhost` do host).

Testando via **Expo Web** (`expo start --web`): ajuste `CorsConfig.java`
se a porta do dev server do Expo Web não for `8081`, ou adicione mais de
uma origem permitida.
 