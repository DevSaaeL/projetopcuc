# Microsoft Entra e locais de atuação

## Ativação no Render

A integração foi implementada com OpenID Connect (authorization code), validação do emissor, assinatura, audiência, estado e nonce pelo Spring Security. O servidor aceita somente o tenant institucional configurado. Não requer permissão de alteração de senhas ou Microsoft Graph.

1. Um administrador do tenant registra um aplicativo **Web, de tenant único** no Microsoft Entra ID.
2. Cadastre exatamente esta URI de redirecionamento:
   `https://projetopcuc.onrender.com/login/oauth2/code/microsoft`
3. Em **Usuários** no HelpDesk, vincule o **ID do objeto** Microsoft de cada usuário ao cadastro correspondente. O e-mail serve para exibição; a autorização usa o ID imutável do objeto e o tenant. Vincule primeiro ao menos um administrador mestre ativo.
4. Em **Environment** do serviço Render, configure `MICROSOFT_TENANT_ID`, `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` e `MICROSOFT_REDIRECT_URI`. O segredo de cliente deve ficar somente nas variáveis protegidas do servidor, nunca no GitHub ou JavaScript.
5. Ative `MICROSOFT_ENABLED=true` e faça o deploy. O login local fica desabilitado. O servidor recusa iniciar com Microsoft ativada se não houver administrador mestre vinculado.
6. Valide com uma conta institucional real e a política de MFA da instituição. A aprovação/consentimento no Azure deve ser feita pelo administrador do tenant.

Sem essas credenciais, `MICROSOFT_ENABLED=false` mantém temporariamente o acesso dos usuários existentes. Novos cadastros não recebem senha local e aguardam ativação Microsoft. O autocadastro e os endpoints locais de alteração de senha ficam desabilitados em ambos os modos.

Em um banco novo, configure `BOOTSTRAP_EMAIL` e `BOOTSTRAP_MICROSOFT_OBJECT_ID` para o primeiro administrador quando Microsoft estiver ativada. O bootstrap não cria faculdades ou unidades. No modo temporário de acesso local, o bootstrap também exige `BOOTSTRAP_PASSWORD`.

## Faculdades, unidades e técnicos

- Somente o administrador mestre cria, altera ou desativa locais, manualmente. No primeiro acesso sem locais, o sistema orienta esse cadastro.
- No cadastro de técnico ou administrador de unidade, selecione uma ou mais unidades. O backend exige ao menos uma e verifica se o cadastrante pode atribuí-las.
- Técnicos visualizam e atendem somente chamados das unidades atribuídas, inclusive por acesso direto à API. A regra também vale para mensagens, histórico, anexos, QR Codes, notificações, dashboard e relatórios.
- Administradores locais não podem atribuir unidades fora do próprio escopo ou alterar usuários que também atendam unidades fora dele.
- O administrador mestre vê todas as unidades. Solicitantes veem seus próprios chamados.
- A migração V5 preserva o vínculo de unidade já existente de cada usuário. Ao alterar vínculos/perfil/status, as sessões anteriores são invalidadas.

## Senhas e MFA

Recuperação: https://passwordreset.microsoftonline.com
Alteração de senha: https://myaccount.microsoft.com
Informações de segurança/MFA: https://mysignins.microsoft.com/security-info

O HelpDesk apenas direciona para esses portais; não recebe nem altera a senha institucional. A disponibilidade de recuperação depende da configuração de SSPR no tenant.

Documentação oficial:
- https://learn.microsoft.com/en-us/entra/identity-platform/v2-protocols-oidc
- https://learn.microsoft.com/en-us/entra/identity-platform/how-to-add-redirect-uri
- https://learn.microsoft.com/en-us/entra/identity-platform/claims-validation
- https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html

## Validação

`mvn test` cobre isolamento entre três unidades, técnico com duas unidades, mudança de escopo, bloqueios de APIs, cadastro obrigatório e exclusividade do administrador mestre. Também cobre o redirecionamento Microsoft, rejeição de callback sem estado válido e vínculo por tenant/ID do objeto. Os testes de identidade usam tokens simulados e não substituem um login real no tenant institucional.
