App.start(async function () {
    const ticketForm = document.getElementById("ticketForm");
    const titulo = document.getElementById("titulo");
    const descricao = document.getElementById("descricao");
    const categoria = document.getElementById("categoria");
    const cidade = document.getElementById("cidade");
    const filial = document.getElementById("filial");
    const bloco = document.getElementById("bloco");
    const sala = document.getElementById("sala");
            const attachmentInput =
                document.getElementById("attachments");

            const attachmentArea =
                document.getElementById("attachmentArea");

            const fileList =
                document.getElementById("fileList");

            const modalSucesso =
                document.getElementById("modalSucesso");

            let arquivosSelecionados = [];


    titulo.addEventListener("input", () => { document.getElementById("tituloCounter").textContent = titulo.value.length; });
    descricao.addEventListener("input", () => { document.getElementById("descricaoCounter").textContent = descricao.value.length; });
            attachmentArea.addEventListener(
                "click",
                function () {

                    attachmentInput.click();

                }
            );


            attachmentInput.addEventListener(
                "change",
                function () {

                    const novosArquivos =
                        Array.from(this.files);


                    novosArquivos.forEach(function (file) {

                        const jaExiste =
                            arquivosSelecionados.some(
                                function (arquivo) {

                                    return (
                                        arquivo.name === file.name &&
                                        arquivo.size === file.size
                                    );

                                }
                            );


                        if (jaExiste) {
                            return;
                        }


                        if (
                            file.size >
                            10 * 1024 * 1024
                        ) {

                            alert(
                                'O arquivo "' +
                                file.name +
                                '" ultrapassa o limite de 10 MB.'
                            );

                            return;

                        }


                        arquivosSelecionados.push(file);

                    });


                    atualizarListaArquivos();

                    attachmentInput.value = "";

                }
            );


            function atualizarListaArquivos() {

                fileList.innerHTML = "";


                arquivosSelecionados.forEach(
                    function (file, index) {

                        const item =
                            document.createElement("div");

                        item.className = "file-item";


                        item.innerHTML = `
                            <div class="file-info">

                                <div class="file-icon">
                                    <i class="bi bi-file-earmark"></i>
                                </div>

                                <div>

                                    <div class="fw-semibold file-name">
                                        ${escapeHtml(file.name)}
                                    </div>

                                    <small class="text-muted">
                                        ${formatFileSize(file.size)}
                                    </small>

                                </div>

                            </div>

                            <button
                                type="button"
                                class="btn btn-sm btn-outline-danger"
                                data-index="${index}"
                                title="Remover arquivo"
                            >
                                <i class="bi bi-trash"></i>
                            </button>
                        `;


                        const botaoRemover =
                            item.querySelector("button");


                        botaoRemover.addEventListener(
                            "click",
                            function () {

                                const index =
                                    Number(
                                        this.dataset.index
                                    );

                                arquivosSelecionados.splice(
                                    index,
                                    1
                                );

                                atualizarListaArquivos();

                            }
                        );


                        fileList.appendChild(item);

                    }
                );

            }


            function formatFileSize(bytes) {

                if (bytes < 1024) {
                    return bytes + " B";
                }

                if (bytes < 1024 * 1024) {

                    return (
                        bytes / 1024
                    ).toFixed(1) + " KB";

                }

                return (
                    bytes / (1024 * 1024)
                ).toFixed(1) + " MB";

            }


            /*
             * ==========================================
             * ENVIO DO CHAMADO
             * ==========================================
             */

            let chamadoCriado = null;
    ticketForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        if (!ticketForm.checkValidity()) {ticketForm.classList.add('was-validated'); return;}
        const button = ticketForm.querySelector('button[type="submit"]');button.disabled=true;
        await App.run(async()=>{
            if (!chamadoCriado) chamadoCriado = await API.post('/chamados', {
                titulo: titulo.value.trim(), descricao: descricao.value.trim(), categoria: categoria.value,
                tipoAtendimento: document.getElementById('tipoAtendimento').value,
                filialId: Number(filial.value), bloco: bloco.value, sala: sala.value,
                qrId: new URLSearchParams(location.search).get('qr')
            });
            // Remove only files acknowledged by the API; a retry does not duplicate the ticket.
            while (arquivosSelecionados.length) {const data=new FormData();data.append('arquivo',arquivosSelecionados[0]);await API.post('/chamados/'+chamadoCriado.id+'/anexos',data);arquivosSelecionados.shift();atualizarListaArquivos();}
            document.getElementById('numeroChamado').textContent=chamadoCriado.protocolo;
            bootstrap.Modal.getOrCreateInstance(modalSucesso).show();
        });
        button.disabled=false;
    });

    /*
             * ==========================================
             * CANCELAR
             * ==========================================
             */

            document
                .getElementById("btnCancelar")
                .addEventListener(
                    "click",
                    function () {

                        const confirmar =
                            confirm(
                                "Deseja cancelar? Todos os dados preenchidos serão perdidos."
                            );


                        if (confirmar) {

                            window.location.href =
                                "chamados.html";

                        }

                    }
                );


            /*
             * ==========================================
             * VER CHAMADO
             * ==========================================
             */

            document
                .getElementById("btnVerChamado")
                .addEventListener(
                    "click",
                    function () {

                        window.location.href =
                            "chamado-detalhes.html?id=" + chamadoCriado.id;

                    }
                );


            /*
             * ==========================================
             * NOVO CHAMADO
             * ==========================================
             */

            document
                .getElementById("btnNovoChamado")
                .addEventListener(
                    "click",
                    function () {

                        window.location.href =
                            "novo-chamado.html";

                    }
                );


            /*
             * ==========================================
             * ESCAPE HTML
             * ==========================================
             */

            function escapeHtml(text) {

                const div =
                    document.createElement("div");

                div.textContent = text;

                return div.innerHTML;

            }


            /*
             * ==========================================
             * QR CODE
             * ==========================================
             */

            const filiais = (await API.get('/filiais')).filter(f=>f.ativo);
    App.options('filial',filiais,'id','nome');
    App.options('cidade',[...new Set(filiais.map(f=>f.cidade))].map(c=>({id:c,nome:c})),'id','nome');
    filial.addEventListener('change',()=>{const f=filiais.find(f=>f.id===Number(filial.value));if(f){cidade.value=f.cidade;}});
    cidade.addEventListener('change',()=>{const f=filiais.find(f=>f.id===Number(filial.value));if(f&&f.cidade!==cidade.value)filial.value='';});
    const qrId=new URLSearchParams(location.search).get('qr');
    if(qrId){
        ticketForm.querySelector('button[type="submit"]').disabled=true;
        const qr=await API.get('/qrcodes/'+encodeURIComponent(qrId));
        for(const [campo,valor,nome] of [[filial,qr.filial_id,qr.filial_nome],[cidade,qr.cidade,qr.cidade],[bloco,qr.bloco,qr.bloco],[sala,qr.sala,qr.sala]]) {
            if(!Array.from(campo.options).some(o=>o.value===String(valor)))campo.add(new Option(nome,String(valor)));
            campo.value=String(valor);campo.disabled=true;
        }
        const info=document.getElementById('qrLocationInfo');info.classList.add('qr-active');info.replaceChildren();
        const icon=document.createElement('i');icon.className='bi bi-qr-code-scan';info.append(icon,document.createTextNode('Localização identificada: '+qr.filial_nome+' — '+(qr.descricao||qr.nome)));
        ticketForm.querySelector('button[type="submit"]').disabled=false;
    }
});
