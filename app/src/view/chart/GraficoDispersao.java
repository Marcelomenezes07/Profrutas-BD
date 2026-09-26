package view.chart;

import dao.CorrelacaoDAO.Ponto;
import util.Estatistica;
import util.Estatistica.Correlacao;
import view.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Grafico de dispersao (scatter plot) com a reta de regressao linear
 * e o coeficiente de correlacao de Pearson.
 */
public class GraficoDispersao extends JPanel {
    private static final int MARGEM_ESQ = 78, MARGEM_DIR = 24, MARGEM_TOPO = 48, MARGEM_BASE = 58;

    private String titulo = "";
    private String eixoX = "", eixoY = "";
    private List<Ponto> pontos = new ArrayList<>();
    private Correlacao correlacao;

    // escala atual (calculada no paint, usada tambem no tooltip)
    private double minX, maxX, minY, maxY;
    private Rectangle area = new Rectangle();

    public GraficoDispersao() {
        setBackground(UI.CARTAO);
        setBorder(BorderFactory.createLineBorder(UI.BORDA));
        setToolTipText("");
    }

    public void setDados(String titulo, String eixoX, String eixoY, List<Ponto> pontos) {
        this.titulo = titulo;
        this.eixoX = eixoX;
        this.eixoY = eixoY;
        this.pontos = pontos;
        List<double[]> xy = new ArrayList<>();
        for (Ponto p : pontos) xy.add(new double[] {p.x(), p.y()});
        this.correlacao = Estatistica.correlacao(xy);
        repaint();
    }

    public Correlacao getCorrelacao() { return correlacao; }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setFont(UI.SUBTITULO);
        g.setColor(UI.TEXTO);
        g.drawString(titulo, 16, 26);

        area = new Rectangle(MARGEM_ESQ, MARGEM_TOPO,
            getWidth() - MARGEM_ESQ - MARGEM_DIR, getHeight() - MARGEM_TOPO - MARGEM_BASE);
        if (pontos.size() < 2) {
            g.setFont(UI.NORMAL);
            g.setColor(UI.TEXTO_SUAVE);
            g.drawString("Sao necessarios pelo menos 2 pontos", area.x + area.width / 2 - 110, area.y + area.height / 2);
            g.dispose();
            return;
        }

        calcularEscala();
        desenharEixos(g);
        desenharReta(g);
        desenharPontos(g);
        desenharLegenda(g);
        g.dispose();
    }

    private void calcularEscala() {
        minX = maxX = pontos.get(0).x();
        minY = maxY = pontos.get(0).y();
        for (Ponto p : pontos) {
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
        }
        // folga de 5% e eixos alinhados a valores "redondos"
        double fx = (maxX - minX) * 0.05, fy = (maxY - minY) * 0.05;
        if (fx == 0) fx = 1;
        if (fy == 0) fy = 1;
        double passoX = passo(maxX - minX + 2 * fx), passoY = passo(maxY - minY + 2 * fy);
        minX = Math.floor((minX - fx) / passoX) * passoX;
        maxX = Math.ceil((maxX + fx) / passoX) * passoX;
        minY = Math.floor((minY - fy) / passoY) * passoY;
        maxY = Math.ceil((maxY + fy) / passoY) * passoY;
        if (minY < 0 && pontos.stream().allMatch(p -> p.y() >= 0)) minY = 0;
        if (minX < 0 && pontos.stream().allMatch(p -> p.x() >= 0)) minX = 0;
    }

    /** Passo "bonito" (1, 2, 5 x 10^k) para ~5 divisoes. */
    private static double passo(double intervalo) {
        double bruto = intervalo / 5;
        double mag = Math.pow(10, Math.floor(Math.log10(bruto)));
        double f = bruto / mag;
        return (f < 1.5 ? 1 : f < 3 ? 2 : f < 7 ? 5 : 10) * mag;
    }

    private int px(double x) { return (int) Math.round(area.x + (x - minX) / (maxX - minX) * area.width); }
    private int py(double y) { return (int) Math.round(area.y + area.height - (y - minY) / (maxY - minY) * area.height); }

    private void desenharEixos(Graphics2D g) {
        g.setFont(UI.PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        double passoX = passo(maxX - minX), passoY = passo(maxY - minY);

        for (double y = Math.ceil(minY / passoY) * passoY; y <= maxY + 1e-9; y += passoY) {
            int yy = py(y);
            g.setColor(UI.BORDA);
            g.drawLine(area.x, yy, area.x + area.width, yy);
            g.setColor(UI.TEXTO_SUAVE);
            String s = UI.numero(y);
            g.drawString(s, area.x - 8 - fm.stringWidth(s), yy + fm.getAscent() / 2 - 1);
        }
        for (double x = Math.ceil(minX / passoX) * passoX; x <= maxX + 1e-9; x += passoX) {
            int xx = px(x);
            g.setColor(UI.BORDA);
            g.drawLine(xx, area.y, xx, area.y + area.height);
            g.setColor(UI.TEXTO_SUAVE);
            String s = UI.numero(x);
            g.drawString(s, xx - fm.stringWidth(s) / 2, area.y + area.height + fm.getAscent() + 6);
        }
        g.setColor(UI.TEXTO_SUAVE);
        g.drawLine(area.x, area.y + area.height, area.x + area.width, area.y + area.height);
        g.drawLine(area.x, area.y, area.x, area.y + area.height);

        // titulos dos eixos
        g.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        fm = g.getFontMetrics();
        g.setColor(UI.TEXTO);
        g.drawString(eixoX, area.x + area.width / 2 - fm.stringWidth(eixoX) / 2, getHeight() - 14);
        AffineTransform original = g.getTransform();
        g.rotate(-Math.PI / 2);
        g.drawString(eixoY, -(area.y + area.height / 2 + fm.stringWidth(eixoY) / 2), 20);
        g.setTransform(original);
    }

    private void desenharReta(Graphics2D g) {
        if (correlacao == null || Double.isNaN(correlacao.b())) return;
        Shape clipAnterior = g.getClip();
        g.clip(area);
        double x1 = minX, x2 = maxX;
        double y1 = correlacao.a() + correlacao.b() * x1;
        double y2 = correlacao.a() + correlacao.b() * x2;
        g.setColor(UI.VERMELHO);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(new Line2D.Double(px(x1), py(y1), px(x2), py(y2)));
        g.setClip(clipAnterior);
    }

    private void desenharPontos(Graphics2D g) {
        Color preenchimento = new Color(UI.PALETA[3].getRed(), UI.PALETA[3].getGreen(), UI.PALETA[3].getBlue(), 150);
        g.setStroke(new BasicStroke(1.2f));
        for (Ponto p : pontos) {
            int x = px(p.x()), y = py(p.y());
            g.setColor(preenchimento);
            g.fillOval(x - 6, y - 6, 12, 12);
            g.setColor(UI.PALETA[3].darker());
            g.drawOval(x - 6, y - 6, 12, 12);
        }
    }

    private void desenharLegenda(Graphics2D g) {
        if (correlacao == null) return;
        String[] linhas = {
            String.format("r de Pearson = %.4f", correlacao.pearson()),
            String.format("R² = %.4f", correlacao.r2()),
            String.format("y = %.3f %s %.3f·x", correlacao.a(), correlacao.b() < 0 ? "-" : "+", Math.abs(correlacao.b())),
            "n = " + correlacao.n() + " pontos",
            Estatistica.interpretar(correlacao.pearson())
        };
        g.setFont(UI.NORMAL);
        FontMetrics fm = g.getFontMetrics();
        int largura = 0;
        for (String s : linhas) largura = Math.max(largura, fm.stringWidth(s));
        largura += 42;
        int altura = linhas.length * 20 + 14;
        // posiciona a legenda no canto oposto a tendencia, para nao cobrir os pontos
        boolean positiva = correlacao.b() >= 0 || Double.isNaN(correlacao.b());
        int x = positiva ? area.x + 12 : area.x + area.width - largura - 12;
        int y = area.y + 10;
        g.setColor(new Color(255, 255, 255, 235));
        g.fillRoundRect(x, y, largura, altura, 10, 10);
        g.setColor(UI.BORDA);
        g.drawRoundRect(x, y, largura, altura, 10, 10);
        g.setColor(UI.VERMELHO);
        g.setStroke(new BasicStroke(2.4f));
        g.drawLine(x + 10, y + 22, x + 26, y + 22);
        for (int i = 0; i < linhas.length; i++) {
            g.setColor(i == linhas.length - 1 ? UI.VERDE_ESCURO : UI.TEXTO);
            g.setFont(i == 0 || i == linhas.length - 1 ? UI.NORMAL.deriveFont(Font.BOLD) : UI.NORMAL);
            g.drawString(linhas[i], x + 32, y + 26 + i * 20);
        }
    }

    /** Mostra o rotulo e os valores do ponto sob o mouse. */
    @Override
    public String getToolTipText(MouseEvent e) {
        if (pontos.size() < 2) return null;
        for (Ponto p : pontos) {
            if (Math.abs(px(p.x()) - e.getX()) <= 7 && Math.abs(py(p.y()) - e.getY()) <= 7) {
                return "<html><b>" + p.rotulo() + "</b><br>x = " + UI.numero(p.x()) + "<br>y = " + UI.numero(p.y()) + "</html>";
            }
        }
        return null;
    }
}
