import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/** Java 21. Câmera na origem, olhando para +Z; X à direita e Y para cima. */
public class Rasterizador {
    record V3(double x, double y, double z) {}
    record Tela(double x, double y, double invZ) {}
    record Triangulo(V3 a, V3 b, V3 c, int cor) {}

    final int largura, altura;
    final double near = 0.1;
    final double focal;
    final BufferedImage imagem;
    final double[] profundidade;

    Rasterizador(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
        focal = altura / (2.0 * Math.tan(Math.toRadians(60) / 2.0));
        imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        profundidade = new double[largura * altura];
        limpar();
    }

    void limpar() {
        Arrays.fill(profundidade, Double.POSITIVE_INFINITY);
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) imagem.setRGB(x, y, 0x0d0711);
        }
    }

    static V3 rotacionarY(V3 v, double angulo) {
        double cos = Math.cos(angulo), sin = Math.sin(angulo);
        return new V3(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }

    static V3 mover(V3 v, double x, double y, double z) {
        return new V3(v.x + x, v.y + y, v.z + z);
    }

    Tela projetar(V3 v) {
        return new Tela(largura / 2.0 + focal * v.x / v.z,
                        altura / 2.0 - focal * v.y / v.z, 1.0 / v.z);
    }

    V3 intersecao(V3 a, V3 b) {
        double t = (near - a.z) / (b.z - a.z);
        return new V3(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y), near);
    }

    // Recorte contra Z = near antes da divisão por Z.
    List<V3> recortar(List<V3> entrada) {
        List<V3> saida = new ArrayList<>();
        V3 anterior = entrada.getLast();
        for (V3 atual : entrada) {
            boolean dentro = atual.z >= near, antesDentro = anterior.z >= near;
            if (dentro != antesDentro) saida.add(intersecao(anterior, atual));
            if (dentro) saida.add(atual);
            anterior = atual;
        }
        return saida;
    }

    void desenhar(Triangulo t) {
        List<V3> vertices = recortar(List.of(t.a, t.b, t.c));
        for (int i = 1; i + 1 < vertices.size(); i++) {
            rasterizar(projetar(vertices.getFirst()), projetar(vertices.get(i)),
                       projetar(vertices.get(i + 1)), t.cor);
        }
    }

    static double borda(Tela a, Tela b, double x, double y) {
        return (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x);
    }

    void rasterizar(Tela a, Tela b, Tela c, int cor) {
        double area = borda(a, b, c.x, c.y);
        if (Math.abs(area) < 1e-10) return;
        int minX = (int) Math.max(0, Math.floor(Math.min(a.x, Math.min(b.x, c.x))));
        int maxX = (int) Math.min(largura - 1, Math.ceil(Math.max(a.x, Math.max(b.x, c.x))));
        int minY = (int) Math.max(0, Math.floor(Math.min(a.y, Math.min(b.y, c.y))));
        int maxY = (int) Math.min(altura - 1, Math.ceil(Math.max(a.y, Math.max(b.y, c.y))));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5, py = y + 0.5;
                double wa = borda(b, c, px, py) / area;
                double wb = borda(c, a, px, py) / area;
                double wc = 1.0 - wa - wb;
                if (wa < 0 || wb < 0 || wc < 0) continue;
                double invZ = wa * a.invZ + wb * b.invZ + wc * c.invZ;
                double z = 1.0 / invZ;
                int indice = y * largura + x;
                if (z < profundidade[indice]) {
                    profundidade[indice] = z;
                    imagem.setRGB(x, y, cor);
                }
            }
        }
    }

    static Triangulo transformar(Triangulo t, double angulo) {
        return new Triangulo(mover(rotacionarY(t.a, angulo), 0, 0, 3),
                             mover(rotacionarY(t.b, angulo), 0, 0, 3),
                             mover(rotacionarY(t.c, angulo), 0, 0, 3), t.cor);
    }

    static void testar() {
        Rasterizador r = new Rasterizador(128, 128);
        Triangulo perto = new Triangulo(new V3(-1, -1, 2), new V3(1, -1, 2),
                                       new V3(0, 1, 2), 0xe5c454);
        Triangulo longe = new Triangulo(new V3(-1, -1, 4), new V3(1, -1, 4),
                                       new V3(0, 1, 4), 0x7c3cb5);
        r.desenhar(perto); r.desenhar(longe);
        int[] primeira = r.imagem.getRGB(0, 0, 128, 128, null, 0, 128);
        verificar((r.imagem.getRGB(64, 64) & 0xffffff) == perto.cor, "oclusão");
        r.limpar(); r.desenhar(longe); r.desenhar(perto);
        verificar(Arrays.equals(primeira, r.imagem.getRGB(0, 0, 128, 128, null, 0, 128)), "ordem");
        List<V3> recorte = r.recortar(List.of(new V3(0, 1, -1), new V3(-1, -1, 2), new V3(1, -1, 2)));
        verificar(recorte.size() == 4 && recorte.stream().allMatch(v -> v.z >= r.near), "recorte");
        verificar(r.recortar(List.of(new V3(0, 0, -1), new V3(1, 0, -1), new V3(0, 1, -1))).isEmpty(), "atrás");
        Tela centro = r.projetar(new V3(0, 0, 3));
        verificar(centro.x == 64 && centro.y == 64, "centro");
        r.limpar();
        r.rasterizar(new Tela(1, 1, 1), new Tela(2, 2, 1), new Tela(3, 3, 1), 0xffffff);
        verificar(Arrays.stream(r.profundidade).allMatch(Double::isInfinite), "degenerado");
        // Coordenadas de tela (20,20), (108,20), (64,108), com pesos conhecidos no pixel (64,64).
        r.rasterizar(new Tela(20, 20, 0.5), new Tela(108, 20, 0.25), new Tela(64, 108, 0.125), 0xffffff);
        double wa = borda(new Tela(108, 20, 0), new Tela(64, 108, 0), 64.5, 64.5) / 7744.0;
        double wb = borda(new Tela(64, 108, 0), new Tela(20, 20, 0), 64.5, 64.5) / 7744.0;
        double esperado = 1 / (wa / 2 + wb / 4 + (1 - wa - wb) / 8);
        verificar(Math.abs(r.profundidade[64 * 128 + 64] - esperado) < 1e-10, "perspectiva");
        System.out.println("6 verificações passaram: oclusão, ordem, recorte, centro, degenerado e perspectiva.");
    }

    static void verificar(boolean condicao, String nome) {
        if (!condicao) throw new AssertionError(nome);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--test")) { testar(); return; }
        Rasterizador r = new Rasterizador(800, 600);
        Triangulo roxo = transformar(new Triangulo(new V3(-1.25, -0.9, 0.6),
                new V3(1.25, -0.9, -0.6), new V3(0, 1.1, 0), 0x7c3cb5), 0.2);
        Triangulo amarelo = transformar(new Triangulo(new V3(-1.1, 0.65, -0.6),
                new V3(1.1, 0.65, 0.6), new V3(0, -1.1, 0), 0xe5c454), 0.2);
        r.desenhar(roxo); r.desenhar(amarelo);
        ImageIO.write(r.imagem, "png", new File("triangulos.png"));
        System.out.println("Imagem gravada em triangulos.png");
    }
}
