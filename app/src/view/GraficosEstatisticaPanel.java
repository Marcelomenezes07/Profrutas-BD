package view;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Galeria com os graficos gerados para a disciplina de Estatistica.
 * Basta colocar as imagens (PNG/JPG) na pasta "graficos" do projeto
 * ou usar o botao "Adicionar imagem".
 */
public class GraficosEstatisticaPanel extends JPanel {
    private static final File PASTA = util.ConnectionFactory.pastaProjeto().resolve("graficos").toFile();
    private final JPanel galeria = new JPanel(new GridLayout(0, 2, 14, 14));

    public GraficosEstatisticaPanel() {
        super(new BorderLayout(0, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(UI.titulo("Graficos - Estatistica"), BorderLayout.WEST);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        JButton adicionar = UI.botao("Adicionar imagem", true);
        adicionar.addActionListener(e -> adicionar());
        JButton recarregar = UI.botao("Recarregar", false);
        recarregar.addActionListener(e -> carregar());
        botoes.add(recarregar);
        botoes.add(adicionar);
        topo.add(botoes, BorderLayout.EAST);

        galeria.setOpaque(false);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(galeria, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        add(topo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void carregar() {
        galeria.removeAll();
        PASTA.mkdirs();
        File[] imagens = PASTA.listFiles(f -> f.getName().toLowerCase().matches(".*\\.(png|jpe?g|gif)$"));
        if (imagens == null || imagens.length == 0) {
            JLabel vazio = new JLabel("<html><div style='text-align:center'>Nenhum grafico encontrado.<br>"
                + "Coloque as imagens dos graficos de Estatistica (PNG/JPG) na pasta:<br><b>"
                + PASTA.getAbsolutePath() + "</b><br>ou clique em \"Adicionar imagem\".</div></html>",
                SwingConstants.CENTER);
            vazio.setFont(UI.NORMAL);
            vazio.setForeground(UI.TEXTO_SUAVE);
            galeria.setLayout(new BorderLayout());
            galeria.add(vazio, BorderLayout.CENTER);
        } else {
            galeria.setLayout(new GridLayout(0, 2, 14, 14));
            Arrays.sort(imagens, Comparator.comparing(File::getName));
            for (File f : imagens) galeria.add(miniatura(f));
        }
        galeria.revalidate();
        galeria.repaint();
    }

    private JComponent miniatura(File arquivo) {
        JPanel card = UI.cartao(new BorderLayout(0, 8));
        String nome = arquivo.getName().replaceFirst("\\.[^.]+$", "").replace('_', ' ');
        JLabel titulo = new JLabel(nome);
        titulo.setFont(UI.SUBTITULO);
        card.add(titulo, BorderLayout.NORTH);
        try {
            BufferedImage img = ImageIO.read(arquivo);
            int largura = 520;
            int altura = img.getHeight() * largura / img.getWidth();
            JLabel imagem = new JLabel(new ImageIcon(img.getScaledInstance(largura, altura, Image.SCALE_SMOOTH)));
            imagem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imagem.setToolTipText("Clique para ampliar");
            imagem.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { ampliar(nome, img); }
            });
            card.add(imagem, BorderLayout.CENTER);
        } catch (Exception e) {
            card.add(new JLabel("Nao foi possivel abrir a imagem."), BorderLayout.CENTER);
        }
        return card;
    }

    private void ampliar(String titulo, BufferedImage img) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), titulo, Dialog.ModalityType.MODELESS);
        Dimension tela = Toolkit.getDefaultToolkit().getScreenSize();
        double escala = Math.min(1.0, Math.min(tela.width * 0.85 / img.getWidth(), tela.height * 0.85 / img.getHeight()));
        Image imagem = img.getScaledInstance((int) (img.getWidth() * escala), (int) (img.getHeight() * escala), Image.SCALE_SMOOTH);
        d.add(new JScrollPane(new JLabel(new ImageIcon(imagem))));
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void adicionar() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens", "png", "jpg", "jpeg", "gif"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        PASTA.mkdirs();
        try {
            for (File f : fc.getSelectedFiles()) {
                Files.copy(f.toPath(), new File(PASTA, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            carregar();
        } catch (IOException e) {
            UI.erro(this, e);
        }
    }
}
