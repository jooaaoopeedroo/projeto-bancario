public class Controlador {
    private static final CentralBancaria central = new CentralBancaria();

    public static void criarConta() {
        Telas.cabecalhoCadastro();
        Cliente cliente = new Cliente();

        // Nome
        String nomeInformado;
        do {
            Telas.limparTela();
            nomeInformado = Telas.lerTexto("Digite o nome completo: ");
            if (!cliente.setNome(nomeInformado)) {
                Telas.mensagem("Nome inválido!", true);
            }
        } while (!cliente.setNome(nomeInformado));

        // CPF
        String cpfInformado;
        do {
            Telas.limparTela();
            cpfInformado = Telas.lerTexto("Digite o CPF: ");
            if (!cliente.setCpf(cpfInformado)) {
                Telas.mensagem("CPF inválido.", true);
            }
        } while (!cliente.setCpf(cpfInformado));

        // Data de Nascimento
        String data;
        do {
            Telas.limparTela();
            data = Telas.lerTexto("Data de nascimento (dd/mm/aaaa)");
            if (!cliente.setDataNascimento(data)) {
                Telas.mensagem("Data de nascimento inválida.", true);
            }
        } while (!cliente.setDataNascimento(data));

        // Envia à CentralBancaria
        System.out.println("Enviando dados para a central...");
        String resultado = central.cadastrar(
                cliente.getNome(),
                cliente.getCpf(),
                cliente.getDataNascimento());

        if (resultado.startsWith("ERRO")) {
            Telas.mensagem(resultado, true);
            return;
        }

        String numeroConta = resultado;

        // Solicitar Senha
        Telas.limparTela();
        System.out.println("Conta criada com sucesso!");
        System.out.println("Número da conta: " + numeroConta);
        Telas.separador();

        String senha, confirma;
        do {
            senha = Telas.lerTexto("Crie sua senha (4 dígitos numéricos)");
            confirma = Telas.lerTexto("Confirme sua senha");
            if (!senha.equals(confirma)) {
                Telas.mensagem("Senhas não conferem. Tente novamente.", true);
            } else if (!senha.matches("\\d{4}")) {
                Telas.mensagem("Senha inválida. Use exatamente 4 dígitos numéricos.", true);
            }
        } while (!senha.equals(confirma) || !senha.matches("\\d{4}"));

        central.cadastrarSenha(numeroConta, senha);
        Telas.mensagem("Cadastro concluído! Número da conta: " + numeroConta, false);
    }

    public static void acessarConta() {
        Telas.cabecalhoLogin();
        String numeroConta = Telas.lerTexto("Número da conta");
        int tentativas = 0;
        while (tentativas < 3) {
            String senha = Telas.lerTexto("Senha: ");
            Cliente cliente = new Cliente();
            String status = central.login(numeroConta, senha, cliente);

            switch (status) {
                case "OK":
                    menuConta(cliente);
                case "CONTA_INEXISTENTE":
                    Telas.mensagem("Conta inexistente. Verifique o número e tente novamente.", true);
                    return;
                case "BLOQUEADA":
                    Telas.mensagem("Conta bloqueada devido a múltiplas tentativas de login falhadas.", true);
                    return;
                case "SENHA_INCORRETA":
                    tentativas++;
                    if (tentativas < 3) {
                        Telas.mensagem("Senha incorreta. Tentativa " + tentativas + " de 3.", false);
                    }
                    break;
                default:
                    Telas.mensagem("Erro de comunicação. Tente novamente mais tarde.", true);
                    return;
            }

        }

    }

    public static void menuConta(Cliente cliente) {
        int opcao;
        do {
            Telas.menuConta(cliente.getNome(), cliente.getSaldo());
            opcao = Telas.lerOpcao();

            switch (opcao) {
                case 1:
                    // Iremos implementar o depósito
                    Telas.mensagem("Iremos implementar o depósito", false);
                    break;
                case 2:
                    // Iremos implementar o saque
                    Telas.mensagem("Iremos implementar o saque", false);
                    break;
                case 3:
                    // Iremos implementar transferência
                    Telas.mensagem("Iremos implementar transferência", false);
                    break;
                case 4:
                    // Iremos implementar o extrato
                    Telas.mensagem("Iremos implementar o extrato", false);
                    break;
                case 5:
                    Telas.mensagem("Até logo, " + cliente.getNome() + "!", false);
                    break;
                default:
                    Telas.mensagem("Opção inválida.", true);
            }

        } while (opcao != 5);
    }

    // Operações

    private static void depositar(Cliente cliente) {
        Telas.limparTela();
        double valor = Telas.lerValor("Valor a ser depositado: R$");

        if (valor <= 0) {
            Telas.mensagem("Valor inválido!", true);
            return;
        }

        boolean ok = central.depositar(cliente, valor);
        if (ok) {
            Telas.mensagem(
                    String.format("Depósito de R$ %.2f realizado com sucesso!", valor),
                    false);
        } else {
            Telas.mensagem("Erro ao realizar o depósito!", true);
        }
    }

    private static void sacar(Cliente cliente) {
        Telas.limparTela();
        double valor = Telas.lerValor("Valor que deseja sacar: R$");

        if (valor <= 0) {
            Telas.mensagem("Valor inválido!", true);
            return;
        }
        if (valor > cliente.getSaldo()) {
            Telas.mensagem("Saldo insuficiente!", true);
            return;
        }

        boolean ok = central.sacar(cliente, valor);
        if (ok) {
            Telas.mensagem(
                    String.format("Saque de R$ %.2f realizado com sucesso!\nSaldo atual: R$ %.2f",
                            valor,
                            cliente.getSaldo()),
                    false);
        } else {
            Telas.mensagem("Erro ao realizar o saque!", true);
        }
    }

}
