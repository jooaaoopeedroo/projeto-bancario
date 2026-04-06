import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class Transacao {
    private String tipo;
    private double valor;
    private LocalDate data;
    private static final DateTimeFormatter FORMATADOR = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public Transacao(String tipo, double valor, String data) {
        this.tipo = tipo;
        this.valor = valor;
        this.data = LocalDate.parse(data, FORMATADOR);
    }

    // Getters
    public String getTipo() {
        return this.tipo;
    }

    public double getValor() {
        return this.valor;
    }

    public String getData() {
        return this.data.format(FORMATADOR);
    }

    // Representação formatada da transação
    @Override
    public String toString() {
        return String.format("%-22s: R$ %10.2f %s",
                this.tipo, this.valor, this.data.format(FORMATADOR));
    }

}