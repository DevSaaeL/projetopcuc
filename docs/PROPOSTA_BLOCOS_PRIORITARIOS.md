# Proposta: blocos prioritários por técnico

Proposta para avaliação. Nenhuma regra de atendimento por bloco foi implementada nesta revisão.

## Separar acesso de responsabilidade

- **Filiais de acesso:** continuam definindo os chamados que o técnico pode consultar e assumir. Um técnico de Curitiba não passa a ver Londrina ao receber um bloco prioritário.
- **Blocos prioritários:** indicam os locais pelos quais o técnico é responsável dentro de uma filial já autorizada. São uma preferência de atendimento, sem exclusividade.
- **Responsável pelo chamado:** continua sendo o técnico que efetivamente assumiu o atendimento. O cadastro de um bloco não atribui automaticamente todos os chamados àquela pessoa.

Exemplo: Ana tem acesso à filial Curitiba e prioridade nos blocos A e B. Bruno tem acesso à mesma filial e prioridade no bloco C. Ambos veem os chamados de Curitiba e podem atender os blocos do colega durante uma ausência.

## Cadastro recomendado

1. O administrador cadastra os blocos dentro da filial, em um catálogo único. Os QR Codes passam a referenciar esse cadastro.
2. No cadastro do técnico, mantém as filiais de atuação e seleciona os blocos prioritários de cada uma.
3. Um bloco pode ter mais de um técnico responsável; um técnico pode ser responsável por vários blocos.
4. Blocos sem responsável continuam visíveis e disponíveis para todos os técnicos autorizados na filial.

Hoje os nomes dos blocos são textos dos QR Codes. Antes da conversão, apresentar uma lista para revisar variações como `Central` e `CENTRAL`, evitando duplicações e alterações por suposição. Preservar os identificadores dos QR Codes já impressos.

## Tela do técnico

- A tela abre mostrando **toda a filial**, sem esconder os chamados dos colegas.
- Chamados dos blocos prioritários recebem a identificação **Minha área**.
- A ordem sugerida é: situações críticas/SLA vencido primeiro, depois afinidade com os blocos e, em seguida, antiguidade. Isso precisa ser confirmado como regra operacional antes da implementação.
- Um filtro opcional **Somente minha área** facilita o foco. **Toda a filial** continua sempre disponível.
- Qualquer técnico autorizado na filial pode assumir chamados de outros blocos. O responsável real e as transferências ficam no histórico.

O destaque para cada técnico não cria uma posição pública diferente para cada pessoa. O acompanhamento do solicitante mantém a fila geral da filial e informa que a posição não é previsão de horário. Se a empresa quiser filas independentes por bloco, isso deve ser uma decisão separada, com revisão da comunicação ao solicitante.

## Ausências e alertas: segunda etapa

Depois de validar a primeira etapa, pode-se incluir disponibilidade/ausência e técnicos substitutos. Chamados sem atendimento por um prazo configurado poderiam receber alertas adicionais. O sistema não deve deixar chamados bloqueados em nome de alguém ausente nem transferir chamados automaticamente sem uma regra acordada.

## Critérios para implementação

- Validar permissões no servidor, inclusive para cadastro e alteração de blocos.
- Impedir a atribuição de blocos de uma filial à qual o técnico não tem acesso.
- Manter a proteção de chamados repetidos por local e assunto.
- Testar cobertura por colegas, técnicos com múltiplas filiais, blocos sem responsável e retirada de permissões.
- Validar com a empresa se prioridade significa somente destaque ou também a ordem sugerida descrita acima.
