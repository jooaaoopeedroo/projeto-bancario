import java.util.Scanner;

public class Telas {

    private static final Scanner scanner = new Scanner(System.in);

    public static void limparTela() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    // MENUS

    public static void menuPrincipal() {
        limparTela();
        System.out.println("\n=============================");
        System.out.println("         BANCO JAVA           ");
        System.out.println("=============================");
        System.out.println("1. Criar conta");
        System.out.println("2. Acessar minha conta");
        System.out.println("3. Encerrar");
        System.out.print("Opção selecionada: ");
    }

    public static void menuConta(String nomeCliente, double saldo) {
        limparTela();
        System.out.println("\n=============================");
        System.out.println("  Bem-vindo, " + nomeCliente);
        System.out.printf("  Saldo: R$ %.2f%n", saldo);
        System.out.println("=============================");
        System.out.println("1. Depositar");
        System.out.println("2. Sacar");
        System.out.println("3. Transferir");
        System.out.println("4. Ver extrato");
        System.out.println("5. Sair");
        System.out.print("Opção selecionada: ");
    }

    public static void cabecalhoCadastro() {
        limparTela();
        System.out.println("\n======== Cadastro ========");
    }

    public static void cabecalhoLogin() {
        limparTela();
        System.out.println("\n======== Login ========");
    }

    // LEITURA DE DADOS

    public static String lerTexto(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    public static int lerOpcao() {
        try {
            int opcao = Integer.parseInt(scanner.nextLine().trim());
            return opcao;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static double lerValor(String label) {
        System.out.print(label + ": R$ ");
        try {
            return Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // MENSAGENS

    public static void mensagem(String texto, boolean eErro) {
        limparTela();

        if (!eErro) {
            System.out.println("\n" + texto);
            System.out.println("\nPressione qualquer tecla para continuar...");
        } else {
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            System.out.println("\n[ERRO] " + texto);
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            System.out.println("\nPressione qualquer tecla para continuar...");
        }
        scanner.nextLine();
    }

    // public static void erro(String texto) {
    // limparTela();
    // System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    // System.out.println("\n[ERRO] " + texto);
    // System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    // System.out.println("\nPressione qualquer tecla para continuar...");
    // scanner.nextLine();
    // }

    public static void separador() {
        System.out.println("--------------------------------------------------");
    }
}
