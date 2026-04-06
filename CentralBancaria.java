import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * CentralBancaria — backend do sistema bancário.
 *
 * Toda a complexidade de rede e persistência fica aqui, invisível para o
 * Controlador. Isso é abstração: os alunos usam métodos simples sem precisar
 * saber como o HTTP ou o JSON funcionam por dentro.
 *
 * Endpoint: https://mockapi.jbmiranda.vps-kinghost.net/senai-banco-digital
 *   POST   → cadastrar nova conta
 *   GET    → buscar contas
 *   PUT /id → atualizar conta existente
 *
 * Números de conta: múltiplos de 10, quatro dígitos, iniciando em 1000.
 */
public class CentralBancaria {

    private static final String BASE_URL =
            "https://mockapi.jbmiranda.vps-kinghost.net/senai-banco-digital";

    private final HttpClient http = HttpClient.newHttpClient();

    // =========================================================
    // HTTP
    // =========================================================

    private String httpGet() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String httpPost(String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String httpPut(String id, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    // =========================================================
    // MODELO INTERNO
    // =========================================================

    private static class Registro {
        String id;
        String nome;
        String cpf;
        String dataNascimento;
        String numeroConta;
        String senha;
        double saldo;
        boolean bloqueada;
        int tentativasFalhas;
        List<String[]> transacoes = new ArrayList<>(); // cada item: [tipo, valor, data]
    }

    // =========================================================
    // PARSING DE JSON
    // Sem bibliotecas externas: regex + balanceamento de chaves.
    // =========================================================

    private String parseStr(String json, String key) {
        Pattern p = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private double parseDouble(String json, String key) {
        Pattern p = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]*)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private boolean parseBool(String json, String key) {
        Pattern p = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(json);
        return m.find() && "true".equals(m.group(1));
    }

    private int parseInt(String json, String key) {
        Pattern p = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /*
     * Extrai o objeto JSON aninhado em "body": {...} usando balanceamento
     * de chaves abertura/fechamento.
     */
    private String extrairBody(String recordJson) {
        int label = recordJson.indexOf("\"body\":");
        if (label == -1) return null;
        int inicio = recordJson.indexOf("{", label + 7);
        if (inicio == -1) return null;
        int profundidade = 0;
        for (int i = inicio; i < recordJson.length(); i++) {
            char c = recordJson.charAt(i);
            if (c == '{') profundidade++;
            else if (c == '}') {
                profundidade--;
                if (profundidade == 0) return recordJson.substring(inicio, i + 1);
            }
        }
        return null;
    }

    /*
     * Extrai o array "transacoes" do body e devolve uma lista de
     * String[] {tipo, valor, data}.
     */
    private List<String[]> parseTransacoes(String bodyJson) {
        List<String[]> lista = new ArrayList<>();
        int idx = bodyJson.indexOf("\"transacoes\"");
        if (idx == -1) return lista;
        int arrInicio = bodyJson.indexOf("[", idx);
        if (arrInicio == -1) return lista;
        int pos = arrInicio + 1;
        while (pos < bodyJson.length()) {
            int objInicio = bodyJson.indexOf("{", pos);
            if (objInicio == -1) break;
            int profundidade = 0;
            int objFim = -1;
            for (int i = objInicio; i < bodyJson.length(); i++) {
                char c = bodyJson.charAt(i);
                if (c == '{') profundidade++;
                else if (c == '}') {
                    profundidade--;
                    if (profundidade == 0) { objFim = i; break; }
                }
            }
            if (objFim == -1) break;
            String obj = bodyJson.substring(objInicio, objFim + 1);
            String tipo  = parseStr(obj, "tipo");
            String valor = String.valueOf(parseDouble(obj, "valor"));
            String data  = parseStr(obj, "data");
            lista.add(new String[]{tipo, valor, data});
            pos = objFim + 1;
        }
        return lista;
    }

    private Registro parseRegistro(String recordJson) {
        String id = parseStr(recordJson, "id");
        if (id == null) return null;
        String body = extrairBody(recordJson);
        if (body == null) return null;
        Registro r = new Registro();
        r.id              = id;
        r.nome            = parseStr(body, "nome");
        r.cpf             = parseStr(body, "cpf");
        r.dataNascimento  = parseStr(body, "dataNascimento");
        r.numeroConta     = parseStr(body, "numeroConta");
        r.senha           = parseStr(body, "senha");
        r.saldo           = parseDouble(body, "saldo");
        r.bloqueada       = parseBool(body, "bloqueada");
        r.tentativasFalhas = parseInt(body, "tentativasFalhas");
        r.transacoes      = parseTransacoes(body);
        return r;
    }

    /*
     * Parseia a resposta do GET: array direto [{...}, {...}]
     */
    private List<Registro> parseTodos(String json) {
        List<Registro> lista = new ArrayList<>();
        int arrInicio = json.indexOf("[");
        if (arrInicio == -1) return lista;
        int pos = arrInicio + 1;
        while (pos < json.length()) {
            int objInicio = json.indexOf("{", pos);
            if (objInicio == -1) break;
            int profundidade = 0;
            int objFim = -1;
            for (int i = objInicio; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') profundidade++;
                else if (c == '}') {
                    profundidade--;
                    if (profundidade == 0) { objFim = i; break; }
                }
            }
            if (objFim == -1) break;
            Registro r = parseRegistro(json.substring(objInicio, objFim + 1));
            if (r != null) lista.add(r);
            pos = objFim + 1;
        }
        return lista;
    }

    // =========================================================
    // CONSTRUÇÃO DE JSON
    // =========================================================

    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildBody(Registro r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"nome\":\"").append(escapar(r.nome)).append("\",");
        sb.append("\"cpf\":\"").append(escapar(r.cpf)).append("\",");
        sb.append("\"dataNascimento\":\"").append(escapar(r.dataNascimento)).append("\",");
        sb.append("\"numeroConta\":\"").append(escapar(r.numeroConta)).append("\",");
        sb.append("\"senha\":\"").append(escapar(r.senha)).append("\",");
        sb.append("\"saldo\":").append(r.saldo).append(",");
        sb.append("\"bloqueada\":").append(r.bloqueada).append(",");
        sb.append("\"tentativasFalhas\":").append(r.tentativasFalhas).append(",");
        sb.append("\"transacoes\":[");
        for (int i = 0; i < r.transacoes.size(); i++) {
            String[] t = r.transacoes.get(i);
            sb.append("{");
            sb.append("\"tipo\":\"").append(escapar(t[0])).append("\",");
            sb.append("\"valor\":").append(t[1]).append(",");
            sb.append("\"data\":\"").append(escapar(t[2])).append("\"");
            sb.append("}");
            if (i < r.transacoes.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private List<Registro> buscarTodos() throws Exception {
        return parseTodos(httpGet());
    }

    private Registro buscarPorConta(String numeroConta) throws Exception {
        for (Registro r : buscarTodos()) {
            if (numeroConta.equals(r.numeroConta)) return r;
        }
        return null;
    }

    private Registro buscarPorCpf(String cpf) throws Exception {
        for (Registro r : buscarTodos()) {
            if (cpf.equals(r.cpf)) return r;
        }
        return null;
    }

    /*
     * Gera o próximo número de conta: múltiplo de 10, 4 dígitos, a partir
     * de 1000. Busca o maior numeroConta já existente e soma 10.
     */
    private String proximaNumeroConta() throws Exception {
        int max = 990; // se não houver contas, a primeira será 1000
        for (Registro r : buscarTodos()) {
            try {
                int n = Integer.parseInt(r.numeroConta);
                if (n >= 1000 && n % 10 == 0 && n > max) max = n;
            } catch (NumberFormatException ignorado) { }
        }
        return String.valueOf(max + 10);
    }

    private String dataAtual() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // =========================================================
    // API PÚBLICA
    // =========================================================

    /**
     * Cadastra uma nova conta com os dados do cliente.
     *
     * @return numeroConta gerado (ex: "1000") em caso de sucesso.
     *         "ERRO:CPF_JA_CADASTRADO" se o CPF já existir.
     *         "ERRO:..." para falhas de comunicação.
     */
    public String cadastrar(String nome, String cpf, String dataNascimento) {
        try {
            if (buscarPorCpf(cpf) != null) return "ERRO:CPF_JA_CADASTRADO";

            Registro r = new Registro();
            r.nome           = nome;
            r.cpf            = cpf;
            r.dataNascimento = dataNascimento;
            r.numeroConta    = proximaNumeroConta();
            r.senha          = "";
            r.saldo          = 0.0;
            r.bloqueada      = false;
            r.tentativasFalhas = 0;

            String resposta = httpPost(buildBody(r));
            if (parseStr(resposta, "id") == null) return "ERRO:FALHA_NO_SERVIDOR";
            return r.numeroConta;
        } catch (Exception e) {
            return "ERRO:" + e.getMessage();
        }
    }

    /**
     * Define a senha de uma conta recém-criada.
     * Deve ser chamado logo após cadastrar() com sucesso.
     *
     * @return true se a senha foi registrada com sucesso.
     */
    public boolean cadastrarSenha(String numeroConta, String senha) {
        try {
            Registro r = buscarPorConta(numeroConta);
            if (r == null) return false;
            r.senha = senha;
            httpPut(r.id, buildBody(r));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Realiza o login do cliente.
     *
     * Possíveis retornos:
     *   "OK"               → login bem-sucedido; cliente é populado com nome,
     *                        numeroConta e saldo.
     *   "CONTA_INEXISTENTE"→ número de conta não encontrado.
     *   "BLOQUEADA"        → conta bloqueada por tentativas excessivas.
     *   "SENHA_INCORRETA"  → senha errada; após 3 erros a conta é bloqueada.
     *   "ERRO:..."         → falha de comunicação.
     *
     * @param cliente objeto a ser populado em caso de sucesso.
     */
    public String login(String numeroConta, String senha, Cliente cliente) {
        try {
            Registro r = buscarPorConta(numeroConta);
            if (r == null) return "CONTA_INEXISTENTE";
            if (r.bloqueada) return "BLOQUEADA";

            if (!senha.equals(r.senha)) {
                r.tentativasFalhas++;
                if (r.tentativasFalhas >= 3) r.bloqueada = true;
                httpPut(r.id, buildBody(r));
                return "SENHA_INCORRETA";
            }

            // Login bem-sucedido: zera tentativas e popula o cliente
            r.tentativasFalhas = 0;
            httpPut(r.id, buildBody(r));

            cliente.setNome(r.nome);
            cliente.setNumeroConta(r.numeroConta);
            cliente.setSaldo(r.saldo);
            if (r.bloqueada) cliente.setBloqueada(true);
            return "OK";
        } catch (Exception e) {
            return "ERRO:" + e.getMessage();
        }
    }

    /**
     * Realiza depósito na conta do cliente.
     * Atualiza o saldo do objeto cliente em caso de sucesso.
     *
     * @return true se o depósito foi realizado com sucesso.
     */
    public boolean depositar(Cliente cliente, double valor) {
        try {
            if (valor <= 0) return false;
            Registro r = buscarPorConta(cliente.getNumeroConta());
            if (r == null) return false;
            r.saldo += valor;
            r.transacoes.add(new String[]{"DEPOSITO", String.valueOf(valor), dataAtual()});
            httpPut(r.id, buildBody(r));
            cliente.setSaldo(r.saldo);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Realiza saque na conta do cliente.
     * O Controlador deve validar saldo suficiente antes de chamar este método.
     * Atualiza o saldo do objeto cliente em caso de sucesso.
     *
     * @return true se o saque foi realizado com sucesso.
     */
    public boolean sacar(Cliente cliente, double valor) {
        try {
            if (valor <= 0) return false;
            Registro r = buscarPorConta(cliente.getNumeroConta());
            if (r == null || r.saldo < valor) return false;
            r.saldo -= valor;
            r.transacoes.add(new String[]{"SAQUE", String.valueOf(valor), dataAtual()});
            httpPut(r.id, buildBody(r));
            cliente.setSaldo(r.saldo);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Realiza transferência entre contas.
     * O Controlador deve validar saldo suficiente antes de chamar este método.
     * Débita a origem e credita o destino em uma única operação lógica.
     *
     * @param contaDestino número da conta de destino (ex: "1010").
     * @return true se a transferência foi realizada com sucesso.
     */
    public boolean transferir(Cliente cliente, String contaDestino, double valor) {
        try {
            if (valor <= 0) return false;
            Registro origem = buscarPorConta(cliente.getNumeroConta());
            if (origem == null || origem.saldo < valor) return false;
            Registro destino = buscarPorConta(contaDestino);
            if (destino == null) return false;

            origem.saldo -= valor;
            origem.transacoes.add(new String[]{"TRANSFERENCIA_ENVIADA",
                    String.valueOf(valor), dataAtual()});
            httpPut(origem.id, buildBody(origem));

            destino.saldo += valor;
            destino.transacoes.add(new String[]{"TRANSFERENCIA_RECEBIDA",
                    String.valueOf(valor), dataAtual()});
            httpPut(destino.id, buildBody(destino));

            cliente.setSaldo(origem.saldo);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna o extrato completo da conta como lista de strings formatadas.
     * Cada linha contém tipo de operação, valor e data.
     */
    public List<String> getExtrato(Cliente cliente) {
        List<String> extrato = new ArrayList<>();
        try {
            Registro r = buscarPorConta(cliente.getNumeroConta());
            if (r == null) {
                extrato.add("Conta não encontrada.");
                return extrato;
            }
            if (r.transacoes.isEmpty()) {
                extrato.add("Nenhuma movimentação registrada.");
                return extrato;
            }
            for (String[] t : r.transacoes) {
                extrato.add(String.format("%-28s  R$ %10.2f   %s",
                        t[0], Double.parseDouble(t[1]), t[2]));
            }
        } catch (Exception e) {
            extrato.add("Erro ao carregar extrato: " + e.getMessage());
        }
        return extrato;
    }
}
