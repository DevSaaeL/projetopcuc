App.start(async()=>{
let dadosQR = null;

function sucessoLeitura(decodedText) {

    try {

        const url =
            new URL(decodedText);

        const parametros =
            url.searchParams;

        const qr =
            parametros.get("qr");

        const unidade =
            parametros.get("unidade");

        const bloco =
            parametros.get("bloco");

        const sala =
            parametros.get("sala");

        if (!qr) {
            throw new Error(
                "QR Code inválido."
            );
        }

        dadosQR = {
            qr,
            filial: parametros.get("filial"),
            filialNome: parametros.get("filialNome"),
            cidade: parametros.get("cidade"),
            descricao: parametros.get("descricao"),
            unidade,
            bloco,
            sala
        };

        let local = parametros.get("filialNome") || unidade || "";

        if (bloco) {
            local += " • " + bloco;
        }

        if (sala) {
            local += " • Sala " + sala;
        }

        document
            .getElementById("localIdentificado")
            .textContent = local;

        document
            .getElementById("resultado")
            .classList.remove("hidden");

        html5QrcodeScanner.clear().catch(() => {});

    } catch (error) {

        mostrarErro(
            "Este QR Code não pertence ao sistema HelpDesk TI."
        );

    }
}

function mostrarErro(mensagem) {

    const erro =
        document.getElementById("erro");

    erro.textContent = mensagem;

    erro.classList.remove("hidden");

    setTimeout(() => {
        erro.classList.add("hidden");
    }, 4000);
}

const html5QrcodeScanner =
    new Html5QrcodeScanner(
        "reader",
        {
            fps: 10,
            qrbox: {
                width: 250,
                height: 250
            }
        },
        false
    );

html5QrcodeScanner.render(
    sucessoLeitura,
    () => {}
);

document
    .getElementById("btnAbrirChamado")
    .addEventListener("click", () => {

        if (!dadosQR) {
            return;
        }

        const parametros =
            new URLSearchParams({
                qr: dadosQR.qr,
                filial: dadosQR.filial || "",
                filialNome: dadosQR.filialNome || "",
                cidade: dadosQR.cidade || "",
                descricao: dadosQR.descricao || "",
                unidade: dadosQR.unidade || "",
                bloco: dadosQR.bloco || "",
                sala: dadosQR.sala || ""
            });

        window.location.href =
            `novo-chamado.html?${parametros.toString()}`;

    });

});
