# HelpDesk TI

Sistema de chamados com frontend HTML/CSS/JavaScript e backend Java 21, Spring Boot, Spring Security, Flyway e PostgreSQL. Inclui cadastro por e-mail e senha.

## Executar localmente

Instale Java 21 e Maven. Na pasta `backend`, execute:

```powershell
$env:BOOTSTRAP_EMAIL = 'admin@example.com'
$env:BOOTSTRAP_PASSWORD = '<defina uma senha com pelo menos 12 caracteres>'
mvn spring-boot:run
```

Abra `http://localhost:8080/pages/login.html`. O perfil de desenvolvimento usa H2 em `backend/data`.

O arquivo `frontend/assets/js/config.js` atualmente aponta para o backend do Railway. Para testar integralmente no computador, configure `apiBase: ''` na sua copia local, usando a API da mesma origem.

## Testes

```powershell
cd backend
mvn test
```

## Deploy pelo GitHub no Railway

Conecte este repositorio ao servico existente e use a branch `main`, com **Root Directory `/`**. O `Dockerfile` na raiz compila o backend e inclui o frontend no mesmo JAR. O Spring Boot respeita a variavel `PORT` fornecida pela hospedagem.

Configure no Railway as variaveis de `backend/.env.example`, incluindo:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL=jdbc:postgresql://<host>:<porta>/<banco>`
- `DATABASE_USER` e `DATABASE_PASSWORD`
- `BOOTSTRAP_EMAIL` e `BOOTSTRAP_PASSWORD` para o primeiro administrador
- `FRONTEND_ORIGINS` com a origem HTTPS do frontend
- `PUBLIC_FRONTEND_URL` com a URL publica de `/pages/novo-chamado.html`
- `SESSION_SECURE=true`

A URL do banco deve usar o formato JDBC acima. Mantenha as credenciais nas variaveis da hospedagem; nao as coloque no repositorio. O perfil `prod` usa PostgreSQL para persistir os dados entre deploys.

Para construir manualmente, execute `docker build -t helpdesk-ti .` na raiz. O arquivo alternativo em `backend/Dockerfile` tambem usa a raiz como contexto: `docker build -f backend/Dockerfile -t helpdesk-ti .`.

Documentacao de referencia: https://docs.railway.com/builds/dockerfiles
