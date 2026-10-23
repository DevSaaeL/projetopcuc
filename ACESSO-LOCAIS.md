# Acesso local e locais de atuação

Técnicos e administradores entram com e-mail e senha. O login Microsoft foi removido. As migrações históricas mantêm a antiga coluna de identidade somente para compatibilidade do banco; ela não concede acesso.

## Cadastro e redefinição

1. O administrador mestre cadastra as unidades em Locais/Filiais.
2. Em Usuários, preencha nome, e-mail, perfil e uma ou mais unidades.
3. Clique em Gerar senha aleatória. O servidor gera 16 caracteres com SecureRandom.
4. Salve e entregue a senha temporária ao usuário por um canal seguro. A senha não é recuperável depois; somente o hash BCrypt fica no banco.
5. No primeiro login, o painel fica bloqueado até criar e confirmar uma senha própria, diferente da temporária: mínimo de 8 caracteres, maiúscula, minúscula, número e caractere especial; máximo de 72 bytes UTF-8. Há botão para mostrar/ocultar as senhas.

Para redefinir o acesso de outra conta, edite o usuário, gere uma nova senha e salve. A operação invalida as sessões anteriores e exige a troca no próximo acesso. O administrador só pode gerenciar usuários dentro das unidades autorizadas; apenas o mestre gerencia outros mestres. A própria senha é alterada em Meu perfil.

As senhas locais existentes continuam funcionando. Uma conta anteriormente usada apenas via Microsoft precisa receber uma senha temporária pelo administrador. Não há autocadastro. Solicitantes leem o QR Code sem login e informam nome e escolhem o assunto.

## Câmera e chamados

Novo chamado usa a câmera para identificar o local; o navegador pede permissão ao clicar no botão. O leitor usa jsQR 1.4.0, distribuído localmente com licença Apache-2.0 em frontend/assets/vendor, sem enviar imagens da câmera ao servidor. URLs de outros sites são rejeitadas. A câmera fecha ao sair da página ou concluir a leitura. A conversa do chamado e suas APIs foram removidas; histórico de ações e anexos continuam disponíveis.

## Banco

Deploys não apagam chamados nem cadastros. Limpezas pontuais são operações administrativas separadas da aplicação e das migrações.

## Fila, prazos e logs

Chamados abertos aparecem primeiro, do mais antigo para o mais novo; empates na abertura usam o número do chamado. A posição considera todos os chamados abertos visíveis nas unidades da conta e permanece a mesma ao filtrar a lista. Chamados em atendimento/aguardando e encerrados ficam depois da fila de abertos. Assumir um chamado remove sua posição e aproxima o próximo da primeira posição. A ordem orienta o atendimento sem bloquear a seleção manual de um chamado.

O prazo para assumir começa na abertura. O prazo para finalizar começa na primeira assunção. Os contadores atualizam a cada segundo, indicam atraso, continuam durante Aguardando usuário e param ao finalizar/cancelar. Reabrir inicia um novo ciclo de SLA; a abertura original e os dados do atendimento anterior ficam preservados no histórico imutável. O dashboard mantém Chamados recentes em ordem de abertura mais recente.

A aba Logs por usuário permite filtrar usuário, ação e período no horário de Brasília, com 50 registros por página. Técnicos consultam suas próprias ações; administradores consultam chamados de suas unidades, e atividades de conta de usuários totalmente sob sua gestão; o mestre consulta todos. Aberturas sem conta aparecem com o nome do solicitante e podem ser filtradas como Solicitantes sem conta. Entradas, saídas e alterações de perfil/senha/sessões passam a ser registradas a partir desta versão, sem armazenar valores de senhas nos logs.

## Assuntos por unidade

O solicitante informa o nome e escolhe um dos assuntos disponíveis, sem escrever a descrição. O QR Code determina a unidade; o servidor valida que o assunto está ativo naquela unidade e registra seu nome no título e na descrição do chamado. A tela Novo chamado da equipe também usa a seleção de assunto após ler o QR Code.

A aba Assuntos dos chamados é exclusiva de administradores. Ambos os perfis administrativos podem cadastrar, editar, desativar e reativar assuntos dentro do próprio escopo de unidades. Os nove assuntos iniciais são criados em todas as unidades existentes pela migração V9 e nas novas unidades no cadastro. A listagem usa ordenação alfabética em português. Alterações no catálogo preservam os títulos e descrições dos chamados antigos.

## Histórico de atendimentos

O painel consulta atualizações a cada 3 segundos, aguardando a conclusão do ciclo anterior para evitar sobreposição. Temporizadores visíveis continuam atualizando a cada segundo. Requisições GET simultâneas para o mesmo endereço compartilham a consulta em andamento, sem manter cache entre ciclos. Listas idênticas não são reconstruídas apenas pela passagem do tempo. O dashboard retorna os cinco chamados recentes exibidos na tela, preservando os totais e os relatórios completos.

Nos novos chamados públicos, o solicitante pode cancelar com motivo obrigatório pela confirmação de envio ou ao retornar ao QR Code no mesmo navegador. O acesso depende de uma chave aleatória exclusiva, armazenada no navegador; o banco guarda somente o hash dessa chave. O protocolo sozinho não autoriza cancelamento. Chamados finalizados não podem ser cancelados. A reabertura por administrador invalida a chave do ciclo anterior. Chamados antigos, criados sem essa chave, continuam dependendo de um administrador para cancelamento.

O cancelamento do solicitante registra o nome, o motivo e a ação `CANCELADO_SOLICITANTE` na auditoria, notifica a equipe da unidade e preserva o encerramento no histórico. Técnicos continuam sem permissão para cancelar pela API da equipe.

Chamados finalizados ou cancelados saem da fila ativa. Técnicos consultam seus próprios atendimentos, inclusive após mudança de unidade; administradores consultam o histórico das unidades permitidas, e o administrador mestre consulta todos. Cada encerramento preserva solução, responsável, datas, histórico e anexos em uma consulta somente leitura. Somente administradores podem reabrir, sem sobrescrever o registro anterior.

## Transferência entre técnicos

O técnico responsável e os administradores podem transferir chamados em atendimento ou aguardando usuário. O destino deve ser outro técnico ativo vinculado à unidade do chamado. O motivo é obrigatório e fica registrado no histórico e nos logs, junto dos responsáveis anterior e novo e de quem realizou a transferência. O novo responsável recebe uma notificação com alerta conforme suas preferências.

A transferência preserva o status e os prazos originais. O responsável anterior perde a permissão de alterar o atendimento; a conclusão é registrada no histórico do novo responsável. Formulários com responsável desatualizado são rejeitados para evitar sobrescrever outra transferência. Chamados abertos sem responsável ou encerrados não podem ser transferidos.
