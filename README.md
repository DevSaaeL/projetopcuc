# HelpDesk TI

Sistema de chamados com frontend HTML/CSS/JavaScript e backend Java 21, Spring Boot, Spring Security, Flyway e PostgreSQL. Acesso local de técnicos e administradores por e-mail e senha; solicitantes abrem chamados por QR Code sem login. Veja [Acesso e locais de atuação](ACESSO-LOCAIS.md).

## Executar localmente

Instale Java 21 e Maven. Na pasta `backend`, execute:

```powershell
$env:BOOTSTRAP_EMAIL = 'admin@example.com'
$env:BOOTSTRAP_PASSWORD = '<defina uma senha com pelo menos 12 caracteres>'
mvn spring-boot:run
```

Abra `http://localhost:8080/pages/login.html`. O perfil de desenvolvimento usa H2 em `backend/data`.

O frontend usa a API da mesma origem (`apiBase: ''`).

## Acompanhamento e gestão

Na lista de chamados ativos, técnicos têm busca, status e período, com identificação de suas filiais de atuação. Técnicos vinculados a uma única filial não veem seletores de cidade, filial ou técnico. Quando há mais de uma filial atribuída, o seletor mostra somente seus locais cadastrados. Os filtros administrativos completos ficam disponíveis aos administradores. A restrição dos dados é aplicada também no servidor, independentemente dos filtros enviados pelo navegador.

A equipe gerencia os atendimentos pelo painel, sem menu ou formulário interno de "Novo chamado". A abertura pelo solicitante começa no QR Code do local. O endereço antigo `novo-chamado.html?qr=...` continua redirecionando ao formulário público, preservando os códigos já impressos. O endereço antigo sem QR Code redireciona à lista de chamados da equipe.

Solicitantes recebem um link público de acompanhamento, sem login, com protocolo, assunto, status e posição na fila da filial. A fila é ordenada pelo início do ciclo e pelo identificador do chamado; inclui todos os chamados não encerrados, inclusive os assumidos e os que aguardam o solicitante. A posição não representa previsão de horário. O acompanhamento atualiza a cada 10 segundos e não expõe nomes, contatos, anexos ou soluções.

A leitura do QR Code mostra os chamados ativos do mesmo local. Uma nova solicitação do mesmo assunto, filial, bloco e sala retorna o chamado ativo existente, sem criar outro ou repetir notificações. Outro assunto ou local continua permitido. Após finalizar ou cancelar, é possível abrir uma nova solicitação. Essa proteção se aplica à abertura pública; a equipe mantém o cadastro manual de chamados. QR Codes diferentes do mesmo local compartilham a verificação, com comparação de bloco e sala sem distinção entre maiúsculas e minúsculas e ignorando espaços nas extremidades.

Administradores acessam **Gestão e saúde** pelo menu. O painel apresenta até 20 locais e assuntos com mais chamados, com filtros por filial e período de abertura, respeitando os locais de atuação. O administrador mestre consulta todas as filiais. A tela de saúde mostra disponibilidade da aplicação e banco, latência, tempo ativo, versão e migração, sem revelar credenciais. Falhas que impeçam autenticação ou acesso à API aparecem como impossibilidade de consultar o sistema. O painel não substitui monitoramento externo nem testes de restauração de backups.

## Executar os testes

Na raiz, execute `node scripts/check-frontend.cjs` para validar os arquivos da interface e `node --test tests/*.test.cjs` para os testes JavaScript. A [revisão técnica](docs/REVISAO_2026-10-27.md) registra a limpeza e as otimizações realizadas. O [guia de blocos prioritários e aparência](docs/PROPOSTA_BLOCOS_PRIORITARIOS.md) explica a configuração implementada e seu uso.

```powershell
cd backend
mvn test
```

## Deploy pelo GitHub no Render

Conecte este repositorio ao servico existente e use a branch `main`, com **Root Directory `/`**. O `Dockerfile` na raiz compila o backend e inclui o frontend no mesmo JAR. O Spring Boot respeita a variavel `PORT` fornecida pela hospedagem.

O `render.yaml` configura `branch: main` e `autoDeployTrigger: commit` para publicar automaticamente a cada push. Em serviços criados manualmente, configure no painel do Render: **Settings → Build & Deploy → Auto-Deploy → On Commit**, com a branch `main`. O repositório deve estar conectado pela integração do GitHub; serviços criados apenas com uma URL pública exigem publicação manual. Em serviços gerenciados por Blueprint, sincronize o `render.yaml` e mantenha o Auto Sync habilitado.

Configure no Render as variaveis de `backend/.env.example`, incluindo:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL=jdbc:postgresql://<host>:<porta>/<banco>`
- `DATABASE_USER` e `DATABASE_PASSWORD`
- `BOOTSTRAP_EMAIL` e `BOOTSTRAP_PASSWORD` para o primeiro administrador
- `FRONTEND_ORIGINS` com a origem HTTPS do frontend
- `PUBLIC_FRONTEND_URL` com a URL publica de `/pages/novo-chamado.html`
- `SESSION_SECURE=true`

A URL do banco deve usar o formato JDBC acima. Mantenha as credenciais nas variaveis da hospedagem; nao as coloque no repositorio. O perfil `prod` usa PostgreSQL para persistir os dados entre deploys.

Para construir manualmente, execute `docker build -t helpdesk-ti .` na raiz. O arquivo alternativo em `backend/Dockerfile` tambem usa a raiz como contexto: `docker build -f backend/Dockerfile -t helpdesk-ti .`.

Documentacao de referencia: https://render.com/docs/docker
