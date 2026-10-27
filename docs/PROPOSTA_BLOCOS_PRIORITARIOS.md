# Blocos prioritários e aparência

Implementado em 27/10/2026.

## Ativar como administrador

1. Entre em **Blocos** no menu lateral e selecione a filial.
2. Confira os blocos importados dos QR Codes e chamados antigos. Variações apenas de maiúsculas/minúsculas e espaços externos são reunidas dentro da mesma filial. Os QR Codes impressos continuam com os mesmos identificadores.
3. Para um novo bloco, informe o nome, mantenha **Bloco ativo** marcado e clique em **Cadastrar bloco**.
4. Acesse **Usuários**, encontre o técnico e clique no lápis para editar.
5. Mantenha as **filiais de atuação** corretas. Em **Blocos prioritários**, marque os blocos de responsabilidade e clique em **Salvar usuário**. Não é necessário gerar outra senha ao editar somente a área de atuação.
6. O técnico deve entrar novamente após a alteração do cadastro. A atualização de usuário encerra as sessões anteriores por segurança.

Um técnico pode ter vários blocos, e um bloco pode ter vários técnicos. Só aparecem blocos ativos das filiais selecionadas. Sem marcações, o técnico atende suas filiais sem preferência por bloco. Administradores de filial gerenciam somente as filiais às quais têm acesso; técnicos não podem alterar o catálogo.

## Usar no atendimento

- **Chamados ativos** abre com todos os chamados das filiais autorizadas.
- Os chamados dos blocos cadastrados para o técnico recebem **Minha área**.
- A ordem da lista do técnico é: prioridade crítica, SLA vencido, demais chamados; dentro de cada grupo, sua área aparece antes, seguida dos mais antigos e do identificador como desempate.
- **Somente minha área** é um filtro opcional. Desmarque para voltar à lista completa e cobrir colegas ausentes.
- Qualquer técnico autorizado na filial pode assumir chamados de qualquer bloco dessa filial. A preferência não atribui chamados automaticamente nem amplia o acesso a outras filiais.
- A posição pública do solicitante permanece na fila geral da filial, sem personalização por técnico.

## QR Codes e manutenção

Na aba **QR Codes**, selecione primeiro a filial e depois um bloco ativo cadastrado. Renomear um bloco atualiza os QR Codes existentes e os chamados ainda ativos; o histórico encerrado é preservado. Desativar o bloco retira a preferência e a seleção para novos QR Codes. QR Codes já impressos continuam funcionando. Ao retirar uma filial do técnico, suas prioridades daquela filial também são retiradas.

## Claro, Escuro e Sistema

O seletor **Aparência** fica no topo das telas, inclusive login e solicitação pública. **Sistema** acompanha a aparência do Windows/navegador; **Claro** e **Escuro** fixam a escolha. A preferência é salva neste navegador e sincronizada entre suas abas. Outros dispositivos têm suas próprias preferências. QR Codes e cartazes mantêm fundo branco para leitura e impressão.

## Validação

Testes cobrem migração dos locais antigos, isolamento de filiais, permissões do catálogo, vínculo dos blocos, compatibilidade com edição por clientes antigos, cobertura por colegas, fila pública, desativação e preservação dos QR Codes, ordenação e persistência de aparência.
