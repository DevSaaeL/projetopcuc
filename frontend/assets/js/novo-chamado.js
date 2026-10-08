App.start(async function () {
    const ticketForm = document.getElementById("ticketForm");
    const titulo = document.getElementById("titulo");
    const descricao = document.getElementById("descricao");
    const cidade = document.getElementById("cidade");
    const filial = document.getElementById("filial");
    const bloco = document.getElementById("bloco");
    const sala = document.getElementById("sala");
    const modalSucesso = document.getElementById("modalSucesso");

    titulo.addEventListener("input", () => { document.getElementById("tituloCounter").textContent = titulo.value.length; });
    descricao.addEventListener("input", () => { document.getElementById("descricaoCounter").textContent = descricao.value.length; });
            let chamadoCriado = null;
    ticketForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        if (!ticketForm.checkValidity()) {ticketForm.classList.add('was-validated'); return;}
        const button = ticketForm.querySelector('button[type="submit"]');button.disabled=true;
        await App.run(async()=>{
            if (!chamadoCriado) chamadoCriado = await API.post('/chamados', {
                titulo: titulo.value.trim(), descricao: descricao.value.trim(),
                filialId: Number(filial.value), bloco: bloco.value, sala: sala.value,
                qrId: new URLSearchParams(location.search).get('qr')
            });
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
