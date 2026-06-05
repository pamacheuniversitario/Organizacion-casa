// ==============================================================================
// Universidad Distrital Francisco José de Caldas
// Autores: Cristian Velosa (20252020066) y Daniel David Granados Rivera (20252020135)
// Fecha de Creación: 28/05/2026 | Modificación: 04/06/2026
// Descripción: Interfaz Gráfica Principal con Separación de Módulos.
//              Estética Oscura Táctica - Inspirada en Agentes de Valorant.
// ==============================================================================
package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import logica.GestorUsuarios;
import logica.Usuario;

public class ControladorEnergiaGUI extends JFrame {

    private Usuario usuarioActivo;
    private GestorUsuarios gestor;
    
    // Paleta de colores Dark Mode - Valorant/Hacker
    private final Color BG_COLOR = new Color(18, 18, 18);
    private final Color PANEL_COLOR = new Color(30, 30, 30);
    private final Color ACCENT_GREEN = new Color(57, 255, 20); 
    private final Color ACCENT_RED = new Color(255, 69, 0);   
    private final Color ACCENT_CYAN = new Color(0, 212, 255); 
    private final Color TEXT_COLOR = Color.WHITE;
 // Variable global para controlar el tiempo de uso diario según la regla activa
    // Por defecto inicia en 8 horas.
    private int horasUsoDiarioGlobal = 8;
    private String reglaActivaActual = "Ninguna (Manual)";
    // Tarifa promedio en Colombia
    private final double TARIFA_KWH = 850.0;

    private CardLayout cardLayout;
    private JPanel panelContenedor;
    private JPanel panelDashboardTarjetas; 
    private JPanel panelResumenTarjetas; 
    private List<PanelDispositivo> listaPanelesDispositivos;
    
    private final float BASE_WIDTH = 1100f;
    private final float BASE_HEIGHT = 700f;

    public ControladorEnergiaGUI(Usuario usuarioActivo, GestorUsuarios gestor) {
        this.usuarioActivo = usuarioActivo;
        this.gestor = gestor;
        this.listaPanelesDispositivos = new ArrayList<>();

        setTitle("SmartHome Manager - Panel de Control - Agente: " + usuarioActivo.getUsername());
        setSize((int)BASE_WIDTH, (int)BASE_HEIGHT);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        cardLayout = new CardLayout();
        panelContenedor = new JPanel(cardLayout);
        panelContenedor.setBackground(BG_COLOR);

        panelDashboardTarjetas = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        panelDashboardTarjetas.setBackground(BG_COLOR);

        panelResumenTarjetas = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        panelResumenTarjetas.setBackground(BG_COLOR);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                float scale = Math.min(getWidth() / BASE_WIDTH, getHeight() / BASE_HEIGHT);
                aplicarEscaladoFuentes(getContentPane(), scale);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmarSalida();
            }
        });

        inicializarMenuLateral();
        construirVistas();
        cargarDispositivosGuardados(); 
    }
    
    
    private JPanel crearPanelReglas() {
        JPanel panelReglas = new JPanel(new BorderLayout(20, 20));
        panelReglas.setBackground(BG_COLOR);
        panelReglas.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblTitulo = new JLabel("Reglas de Automatización y Horarios");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(TEXT_COLOR);
        panelReglas.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridLayout(1, 3, 20, 20));
        panelCentro.setBackground(BG_COLOR);

        // --- TARJETA 1: ALTO CONSUMO ---
        JPanel cardAlto = crearTarjetaRegla("Alto Consumo", 
            "Mantiene los dispositivos encendidos desde la madrugada hasta las 12 PM (12 hrs diarias).", 
            ACCENT_RED);
        JButton btnAlto = new JButton("Activar Regla");
        estilizarBotonGlobal(btnAlto, ACCENT_RED);
        btnAlto.addActionListener(e -> aplicarReglaGlobal("Alto Consumo", 12));
        cardAlto.add(btnAlto, BorderLayout.SOUTH);

        // --- TARJETA 2: MODO ECO ---
        JPanel cardEco = crearTarjetaRegla("Modo Eco", 
            "Optimiza el uso. Apaga todos los dispositivos estrictamente a las 8 PM (8 hrs diarias).", 
            ACCENT_GREEN);
        JButton btnEco = new JButton("Activar Regla");
        estilizarBotonGlobal(btnEco, ACCENT_GREEN);
        btnEco.addActionListener(e -> aplicarReglaGlobal("Modo Eco", 8));
        cardEco.add(btnEco, BorderLayout.SOUTH);

        // --- TARJETA 3: PERSONALIZADO ---
        JPanel cardCustom = crearTarjetaRegla("Horario Personalizado", 
            "Define tus propias horas de encendido y apagado.", 
            ACCENT_CYAN);
        
        JPanel pnlInputs = new JPanel(new GridLayout(2, 2, 5, 5));
        pnlInputs.setBackground(PANEL_COLOR);
        
        JLabel lblInicio = new JLabel("Hora Encendido:");
        lblInicio.setForeground(Color.LIGHT_GRAY);
        JComboBox<Integer> comboInicio = new JComboBox<>();
        
        JLabel lblFin = new JLabel("Hora Apagado:");
        lblFin.setForeground(Color.LIGHT_GRAY);
        JComboBox<Integer> comboFin = new JComboBox<>();

        // Llenar horas (0 a 23)
        for (int i = 0; i < 24; i++) {
            comboInicio.addItem(i);
            comboFin.addItem(i);
        }
        
        pnlInputs.add(lblInicio);
        pnlInputs.add(comboInicio);
        pnlInputs.add(lblFin);
        pnlInputs.add(comboFin);

        JButton btnCustom = new JButton("Aplicar Horario");
        estilizarBotonGlobal(btnCustom, ACCENT_CYAN);
        btnCustom.addActionListener(e -> {
            int inicio = (int) comboInicio.getSelectedItem();
            int fin = (int) comboFin.getSelectedItem();
            int horasCalculadas = fin - inicio;
            
            if (horasCalculadas <= 0) {
                JOptionPane.showMessageDialog(this, "La hora de apagado debe ser mayor a la de encendido.", "Error de Lógica", JOptionPane.ERROR_MESSAGE);
                return;
            }
            aplicarReglaGlobal("Personalizado (" + inicio + "h a " + fin + "h)", horasCalculadas);
        });

        JPanel wrapCustom = new JPanel(new BorderLayout(0, 10));
        wrapCustom.setBackground(PANEL_COLOR);
        wrapCustom.add(pnlInputs, BorderLayout.CENTER);
        wrapCustom.add(btnCustom, BorderLayout.SOUTH);
        cardCustom.add(wrapCustom, BorderLayout.SOUTH);

        panelCentro.add(cardAlto);
        panelCentro.add(cardEco);
        panelCentro.add(cardCustom);

        panelReglas.add(panelCentro, BorderLayout.CENTER);

        return panelReglas;
    }

    // Método auxiliar para mantener la estética limpia al crear tarjetas de reglas
    private JPanel crearTarjetaRegla(String titulo, String desc, Color colorBorde) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorBorde, 2, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTit.setForeground(TEXT_COLOR);

        JTextArea txtDesc = new JTextArea(desc);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setLineWrap(true);
        txtDesc.setOpaque(false);
        txtDesc.setEditable(false);
        txtDesc.setFocusable(false);
        txtDesc.setForeground(Color.LIGHT_GRAY);
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        panel.add(lblTit, BorderLayout.NORTH);
        panel.add(txtDesc, BorderLayout.CENTER);

        return panel;
    }

    // Lógica que actualiza el estado global y notifica al usuario
    private void aplicarReglaGlobal(String nombreRegla, int horasDia) {
        this.reglaActivaActual = nombreRegla;
        this.horasUsoDiarioGlobal = horasDia;
        
        UIManager.put("OptionPane.background", BG_COLOR);
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
        
        JOptionPane.showMessageDialog(this, 
            "Regla aplicada: " + nombreRegla + "\nLos cálculos de resumen usarán " + horasDia + " horas diarias.", 
            "Regla Actualizada", JOptionPane.INFORMATION_MESSAGE);
    }
    private void inicializarMenuLateral() {
        JPanel panelMenu = new JPanel(new GridLayout(6, 1, 10, 15));
        panelMenu.setBackground(new Color(12, 12, 12));
        panelMenu.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelMenu.setPreferredSize(new Dimension(250, 0));

        JLabel lblTitulo = new JLabel("Smart Home", SwingConstants.CENTER);
        lblTitulo.setForeground(new Color(212, 175, 55)); 
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        panelMenu.add(lblTitulo);

        JButton btnDashboard = crearBotonMenu("Dashboard", ACCENT_CYAN);
        JButton btnDispositivos = crearBotonMenu("Añadir Dispositivos", new Color(165, 230, 40));
        JButton btnReglas = crearBotonMenu("Reglas y Horarios", new Color(74, 82, 138));
        JButton btnResumen = crearBotonMenu("Resumen Total ($)", new Color(254, 150, 180)); 

        // ACCIONES DE LOS BOTONES
        btnDashboard.addActionListener(e -> cardLayout.show(panelContenedor, "VISTA_DASHBOARD"));
        btnDispositivos.addActionListener(e -> cardLayout.show(panelContenedor, "VISTA_DISPOSITIVOS"));
        
        // ¡Aquí está la línea que faltaba para que funcione el botón!
        btnReglas.addActionListener(e -> cardLayout.show(panelContenedor, "VISTA_REGLAS"));
        
        btnResumen.addActionListener(e -> {
            construirTarjetasResumen();
            cardLayout.show(panelContenedor, "VISTA_RESUMEN");
        });

        panelMenu.add(btnDashboard);
        panelMenu.add(btnDispositivos);
        panelMenu.add(btnReglas);
        panelMenu.add(btnResumen);

        JButton btnSalir = crearBotonMenu("Salir del Sistema", ACCENT_RED);
        btnSalir.addActionListener(e -> confirmarSalida());
        panelMenu.add(btnSalir);

        add(panelMenu, BorderLayout.WEST);
    }
    private void construirVistas() {
        JPanel vistaDashboard = crearPanelDashboard();
        JPanel vistaDispositivos = crearPanelDispositivos();
        JPanel vistaResumen = crearPanelResumenGlobal();
        // REEMPLAZAR EL PLACEHOLDER POR ESTA LÍNEA:
        JPanel vistaReglas = crearPanelReglas(); 

        panelContenedor.add(vistaDashboard, "VISTA_DASHBOARD");
        panelContenedor.add(vistaDispositivos, "VISTA_DISPOSITIVOS");
        panelContenedor.add(vistaResumen, "VISTA_RESUMEN");
        panelContenedor.add(vistaReglas, "VISTA_REGLAS");

        cardLayout.show(panelContenedor, "VISTA_DASHBOARD");
        add(panelContenedor, BorderLayout.CENTER);
    }

    private JPanel crearPanelDashboard() {
        JPanel panelDash = new JPanel(new BorderLayout(20, 20));
        panelDash.setBackground(BG_COLOR);
        panelDash.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblDashboard = new JLabel("Dashboard: Control de Aparatos (" + usuarioActivo.getUsername() + ")");
        lblDashboard.setForeground(TEXT_COLOR);
        lblDashboard.setFont(new Font("Segoe UI", Font.BOLD, 26));
        panelDash.add(lblDashboard, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(panelDashboardTarjetas);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        panelDash.add(scrollPane, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new GridLayout(1, 2, 20, 0));
        panelInferior.setBackground(BG_COLOR);

        JButton btnEncenderTodos = new JButton("Encender Todos los Dispositivos");
        estilizarBotonGlobal(btnEncenderTodos, ACCENT_GREEN);
        btnEncenderTodos.addActionListener(e -> accionarTodos(true));

        JButton btnApagarTodos = new JButton("Apagar Todos (Modo Ahorro Crítico)");
        estilizarBotonGlobal(btnApagarTodos, ACCENT_RED);
        btnApagarTodos.addActionListener(e -> accionarTodos(false));

        panelInferior.add(btnEncenderTodos);
        panelInferior.add(btnApagarTodos);
        panelDash.add(panelInferior, BorderLayout.SOUTH);

        return panelDash;
    }

    private JPanel crearPanelDispositivos() {
        JPanel panelDisp = new JPanel(new BorderLayout(20, 20));
        panelDisp.setBackground(BG_COLOR);
        panelDisp.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblTitulo = new JLabel("Administración y Registro de Dispositivos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(TEXT_COLOR);
        panelDisp.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridBagLayout());
        panelCentro.setBackground(BG_COLOR);

        JPanel panelControlAgregar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panelControlAgregar.setBackground(PANEL_COLOR);
        panelControlAgregar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(165, 230, 40), 2, true),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        JLabel lblSeleccionar = new JLabel("Seleccione el dispositivo a vincular:");
        lblSeleccionar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSeleccionar.setForeground(TEXT_COLOR);
        
        String[] opcionesDispositivos = {
            "1. Bombillo LED", "2. Televisor Smart", "3. Nevera Inverter", "4. Calefactor", 
            "5. Consola de Videojuegos", "6. Microondas", "7. Lavadora", "8. Aire Acondicionado", 
            "9. Secador de Cabello", "10. Ventilador", "11. Licuadora", "12. Cargador de Celular", 
            "13. Computador Portátil", "14. Ducha Eléctrica", "15. Air Fryer"
        };
        JComboBox<String> comboDispositivos = new JComboBox<>(opcionesDispositivos);
        comboDispositivos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboDispositivos.setBackground(new Color(40, 40, 40));
        comboDispositivos.setForeground(TEXT_COLOR);

        JButton btnAgregar = new JButton("Vincular al Sistema");
        btnAgregar.setBackground(BG_COLOR);
        btnAgregar.setForeground(new Color(165, 230, 40));
        btnAgregar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAgregar.setBorder(BorderFactory.createLineBorder(new Color(165, 230, 40), 2));
        btnAgregar.setFocusPainted(false);

        panelControlAgregar.add(lblSeleccionar);
        panelControlAgregar.add(comboDispositivos);
        panelControlAgregar.add(btnAgregar);
        
        panelCentro.add(panelControlAgregar);
        panelDisp.add(panelCentro, BorderLayout.CENTER);

        btnAgregar.addActionListener(e -> {
            String seleccion = (String) comboDispositivos.getSelectedItem();
            String nombreDispositivo = seleccion.split("\\.\\s+")[1]; 
            int wattsReales = 0;
            Color colorBorde = ACCENT_CYAN;

            switch (seleccion) {
                case "1. Bombillo LED": wattsReales = 15; colorBorde = ACCENT_GREEN; break;
                case "2. Televisor Smart": wattsReales = 120; colorBorde = ACCENT_CYAN; break;
                case "3. Nevera Inverter": wattsReales = 250; colorBorde = new Color(212, 175, 55); break;
                case "4. Calefactor": wattsReales = 1500; colorBorde = ACCENT_RED; break;
                case "5. Consola de Videojuegos": wattsReales = 200; colorBorde = new Color(180, 120, 220); break;
                case "6. Microondas": wattsReales = 1200; colorBorde = Color.ORANGE; break;
                case "7. Lavadora": wattsReales = 500; colorBorde = new Color(30, 144, 255); break;
                case "8. Aire Acondicionado": wattsReales = 1800; colorBorde = ACCENT_CYAN; break;
                case "9. Secador de Cabello": wattsReales = 1600; colorBorde = Color.PINK; break;
                case "10. Ventilador": wattsReales = 70; colorBorde = ACCENT_GREEN; break;
                case "11. Licuadora": wattsReales = 400; colorBorde = Color.YELLOW; break;
                case "12. Cargador de Celular": wattsReales = 20; colorBorde = Color.LIGHT_GRAY; break;
                case "13. Computador Portátil": wattsReales = 90; colorBorde = new Color(0, 255, 200); break;
                case "14. Ducha Eléctrica": wattsReales = 4500; colorBorde = ACCENT_RED; break; 
                case "15. Air Fryer": wattsReales = 1500; colorBorde = Color.MAGENTA; break;
            }

            String dataCompacta = nombreDispositivo + "#" + wattsReales + "#" + colorBorde.getRGB();
            usuarioActivo.getMisDispositivos().add(dataCompacta);
            gestor.guardarDatos(); 

            PanelDispositivo nuevoPanel = new PanelDispositivo(nombreDispositivo, wattsReales, colorBorde, dataCompacta);
            listaPanelesDispositivos.add(nuevoPanel);
            panelDashboardTarjetas.add(nuevoPanel);
            
            panelDashboardTarjetas.revalidate();
            panelDashboardTarjetas.repaint();

            JOptionPane.showMessageDialog(panelDisp, nombreDispositivo + " vinculado con éxito.");
            cardLayout.show(panelContenedor, "VISTA_DASHBOARD");
        });

        return panelDisp;
    }

    private JPanel crearPanelResumenGlobal() {
        JPanel panelResumen = new JPanel(new BorderLayout(20, 20));
        panelResumen.setBackground(BG_COLOR);
        panelResumen.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblTitulo = new JLabel("Resumen Total de Gastos Estimados (Mensual)");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(TEXT_COLOR);
        panelResumen.add(lblTitulo, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(panelResumenTarjetas);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        panelResumen.add(scrollPane, BorderLayout.CENTER);

        return panelResumen;
    }
    

    private void construirTarjetasResumen() {
        panelResumenTarjetas.removeAll();
        double totalKwhMes = 0;
        double totalPlataMes = 0;

        for (PanelDispositivo disp : listaPanelesDispositivos) {
            // AQUÍ ESTÁ EL CAMBIO: Usamos horasUsoDiarioGlobal en lugar de 8 estático
            double kwhDisp = (disp.getWatts() * horasUsoDiarioGlobal * 30) / 1000.0;
            double costoDisp = kwhDisp * TARIFA_KWH;

            totalKwhMes += kwhDisp;
            totalPlataMes += costoDisp;

            JPanel tarjetica = new JPanel(new GridLayout(4, 1));
            tarjetica.setPreferredSize(new Dimension(220, 180));
            tarjetica.setBackground(PANEL_COLOR);
            tarjetica.setBorder(BorderFactory.createLineBorder(disp.getColorBorde(), 2));

            JLabel lNombre = new JLabel(disp.getNombre(), SwingConstants.CENTER);
            lNombre.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lNombre.setForeground(TEXT_COLOR);

            JLabel lWatts = new JLabel("Potencia: " + disp.getWatts() + "W", SwingConstants.CENTER);
            lWatts.setForeground(Color.LIGHT_GRAY);

            JLabel lKwh = new JLabel(String.format("KWh Mes: %.2f", kwhDisp), SwingConstants.CENTER);
            lKwh.setForeground(ACCENT_CYAN);

            JLabel lCosto = new JLabel(String.format("$ %,.0f COP", costoDisp), SwingConstants.CENTER);
            lCosto.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lCosto.setForeground(ACCENT_GREEN);

            tarjetica.add(lNombre);
            tarjetica.add(lWatts);
            tarjetica.add(lKwh);
            tarjetica.add(lCosto);

            panelResumenTarjetas.add(tarjetica);
        }

        JPanel tarjetaSuma = new JPanel(new GridLayout(4, 1));
        tarjetaSuma.setPreferredSize(new Dimension(350, 200));
        tarjetaSuma.setBackground(new Color(40, 20, 20)); 
        tarjetaSuma.setBorder(BorderFactory.createLineBorder(ACCENT_RED, 4));

        JLabel tTitulo = new JLabel("TOTAL ESTIMADO DEL MES", SwingConstants.CENTER);
        tTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tTitulo.setForeground(Color.YELLOW);

        JLabel tAviso = new JLabel("(Regla Activa: " + reglaActivaActual + " - " + horasUsoDiarioGlobal + "h/día)", SwingConstants.CENTER);
        tAviso.setForeground(Color.LIGHT_GRAY);

        JLabel tKwh = new JLabel(String.format("Total Consumo: %.2f kWh", totalKwhMes), SwingConstants.CENTER);

        JLabel tCosto = new JLabel(String.format("FACTURA: $ %,.0f COP", totalPlataMes), SwingConstants.CENTER);
        tCosto.setFont(new Font("Segoe UI", Font.BOLD, 24));
        tCosto.setForeground(ACCENT_GREEN);

        tarjetaSuma.add(tTitulo);
        tarjetaSuma.add(tAviso);
        tarjetaSuma.add(tKwh);
        tarjetaSuma.add(tCosto);

        panelResumenTarjetas.add(tarjetaSuma);

        panelResumenTarjetas.revalidate();
        panelResumenTarjetas.repaint();
    }

    private void cargarDispositivosGuardados() {
        if (usuarioActivo.getMisDispositivos() != null) {
            for (String d : usuarioActivo.getMisDispositivos()) {
                try {
                    String[] partes = d.split("#");
                    String nombre = partes[0];
                    int watts = Integer.parseInt(partes[1]);
                    Color color = new Color(Integer.parseInt(partes[2]));
                    
                    PanelDispositivo panel = new PanelDispositivo(nombre, watts, color, d);
                    listaPanelesDispositivos.add(panel);
                    panelDashboardTarjetas.add(panel);
                } catch (Exception ex) {}
            }
            panelDashboardTarjetas.revalidate();
            panelDashboardTarjetas.repaint();
        }
    }

    private void accionarTodos(boolean encender) {
        for (PanelDispositivo panel : listaPanelesDispositivos) {
            if (!panel.isBloqueado()) {
                panel.setEstado(encender);
            }
        }
    }

    private void confirmarSalida() {
        UIManager.put("OptionPane.background", BG_COLOR);
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
        
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres cerrar el sistema, mi pez?",
                "Confirmar Salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opcion == JOptionPane.YES_OPTION) System.exit(0);
    }

    private JButton crearBotonMenu(String texto, Color borderColor) {
        JButton btn = new JButton(texto);
        btn.setBackground(BG_COLOR);
        btn.setForeground(TEXT_COLOR);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void GridBagConstraints() {}

    private void estilizarBotonGlobal(JButton btn, Color color) {
        btn.setBackground(BG_COLOR);
        btn.setForeground(color);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(color, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 50));
    }

    private JPanel crearPanelPlaceholder(String msj) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_COLOR);
        JLabel l = new JLabel(msj);
        l.setForeground(TEXT_COLOR);
        l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        p.add(l);
        return p;
    }

    private void aplicarEscaladoFuentes(Component comp, float scale) {
        if (comp.getFont() != null) {
            Float baseSize = (Float) ((JComponent) comp).getClientProperty("baseFontSize");
            if (baseSize == null) {
                baseSize = (float) comp.getFont().getSize();
                ((JComponent) comp).putClientProperty("baseFontSize", baseSize);
            }
            float newSize = Math.max(10f, baseSize * scale);
            comp.setFont(comp.getFont().deriveFont(newSize));
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                aplicarEscaladoFuentes(child, scale);
            }
        }
    }

    // ================= TARJETA DE DISPOSITIVO INTERNA =================
    class PanelDispositivo extends JPanel {
        private boolean encendido = true;
        private boolean bloqueado = false;
        private String nombre;
        private int watts; 
        private String rawData; 
        private Color colorBorde;
        
        private JLabel lblEstado;
        private JLabel lblNombre;
        private JButton btnCandado;
        private JButton btnEditar;

        private ImageIcon imgCandadoVerde;
        private ImageIcon imgCandadoRojo;
        private ImageIcon imgLapiz;

        public PanelDispositivo(String nombre, int watts, Color colorBorde, String rawData) {
            this.nombre = nombre;
            this.watts = watts;
            this.rawData = rawData;
            this.colorBorde = colorBorde;

            // Nombres fijos y limpios vinculados directamente a la raíz de tu proyecto
            imgCandadoVerde = redimensionarIcono("candado_verde.png", 24, 24);
            imgCandadoRojo = redimensionarIcono("candado_rojo.png", 24, 24);
            imgLapiz = redimensionarIcono("lapiz_editar.png", 20, 20);

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(280, 400));
            setBackground(PANEL_COLOR);
            setBorder(BorderFactory.createLineBorder(colorBorde, 2));

            JPanel panelTop = new JPanel(new BorderLayout());
            panelTop.setBackground(PANEL_COLOR);
            panelTop.setBorder(new EmptyBorder(5, 5, 5, 5));

            JPanel pnlIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            pnlIzq.setBackground(PANEL_COLOR);

            // Botón Candado
            btnCandado = new JButton();
            btnCandado.setBackground(PANEL_COLOR);
            btnCandado.setBorder(null);
            btnCandado.setFocusPainted(false);
            btnCandado.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (imgCandadoVerde != null) {
                btnCandado.setIcon(imgCandadoVerde);
            } else {
                // Respaldo de texto seguro si el IDE no renderiza el icono
                btnCandado.setText("[ABRIR]");
                btnCandado.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnCandado.setForeground(ACCENT_GREEN);
            }
            btnCandado.addActionListener(e -> toggleBloqueo());

            // Botón Editar Nombre
            btnEditar = new JButton(); 
            btnEditar.setBackground(PANEL_COLOR);
            btnEditar.setBorder(null);
            btnEditar.setFocusPainted(false);
            btnEditar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (imgLapiz != null) {
                btnEditar.setIcon(imgLapiz);
            } else {
                // Respaldo de texto seguro para el lápiz
                btnEditar.setText("[EDIT]");
                btnEditar.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnEditar.setForeground(ACCENT_CYAN);
            }
            btnEditar.addActionListener(e -> editarNombre());

            pnlIzq.add(btnCandado);
            pnlIzq.add(btnEditar);

            JButton btnCerrar = new JButton("X");
            btnCerrar.setBackground(PANEL_COLOR);
            btnCerrar.setForeground(ACCENT_RED);
            btnCerrar.setBorder(null);
            btnCerrar.setFocusPainted(false);
            btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btnCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCerrar.addActionListener(e -> eliminarDispositivo());

            panelTop.add(pnlIzq, BorderLayout.WEST);
            panelTop.add(btnCerrar, BorderLayout.EAST);
            add(panelTop, BorderLayout.NORTH);

            JPanel panelInfo = new JPanel(new GridLayout(4, 1));
            panelInfo.setBackground(PANEL_COLOR);

            lblNombre = new JLabel(nombre, SwingConstants.CENTER);
            lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblNombre.setForeground(TEXT_COLOR);

            JLabel lblWatts = new JLabel("Potencia: " + watts + " W", SwingConstants.CENTER);
            lblWatts.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            lblWatts.setForeground(Color.LIGHT_GRAY);

            lblEstado = new JLabel("ENCENDIDO", SwingConstants.CENTER);
            lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblEstado.setForeground(ACCENT_GREEN);
            lblEstado.setBorder(BorderFactory.createLineBorder(ACCENT_GREEN, 1));

            JButton btnDetalles = new JButton("Ver Detalles de Consumo");
            btnDetalles.setBackground(BG_COLOR);
            btnDetalles.setForeground(TEXT_COLOR);
            btnDetalles.setFocusPainted(false);
            btnDetalles.addActionListener(e -> mostrarCalculadoraHogar());

            panelInfo.add(lblNombre);
            panelInfo.add(lblWatts);
            panelInfo.add(lblEstado);
            
            JPanel pnlBtnDetalle = new JPanel(); 
            pnlBtnDetalle.setBackground(PANEL_COLOR);
            pnlBtnDetalle.add(btnDetalles);
            panelInfo.add(pnlBtnDetalle);

            add(panelInfo, BorderLayout.CENTER);

            JButton btnToggle = new JButton("ON / OFF");
            btnToggle.setBackground(BG_COLOR);
            btnToggle.setForeground(TEXT_COLOR);
            btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            btnToggle.setBorder(BorderFactory.createLineBorder(TEXT_COLOR, 1));
            btnToggle.setPreferredSize(new Dimension(0, 40));
            btnToggle.setFocusPainted(false);
            btnToggle.addActionListener(e -> setEstado(!encendido));

            add(btnToggle, BorderLayout.SOUTH);
        }

        public String getNombre() { return nombre; }
        public int getWatts() { return watts; }
        public Color getColorBorde() { return colorBorde; }
        public boolean isBloqueado() { return bloqueado; }

        private void toggleBloqueo() {
            bloqueado = !bloqueado;
            if (bloqueado) {
                if (imgCandadoRojo != null) {
                    btnCandado.setIcon(imgCandadoRojo);
                    btnCandado.setText("");
                } else {
                    btnCandado.setText("[BLOQ]");
                    btnCandado.setForeground(ACCENT_RED);
                }
            } else {
                if (imgCandadoVerde != null) {
                    btnCandado.setIcon(imgCandadoVerde);
                    btnCandado.setText("");
                } else {
                    btnCandado.setText("[ABRIR]");
                    btnCandado.setForeground(ACCENT_GREEN);
                }
            }
        }

        private void editarNombre() {
            UIManager.put("OptionPane.background", BG_COLOR);
            UIManager.put("Panel.background", BG_COLOR);
            UIManager.put("OptionPane.messageForeground", TEXT_COLOR);

            String nuevoNombre = JOptionPane.showInputDialog(this, "Renombrar dispositivo:", nombre);
            if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
                String nuevaRawData = nuevoNombre.trim() + "#" + this.watts + "#" + this.colorBorde.getRGB();
                
                usuarioActivo.getMisDispositivos().remove(this.rawData);
                usuarioActivo.getMisDispositivos().add(nuevaRawData);
                gestor.guardarDatos();
                
                this.nombre = nuevoNombre.trim();
                this.rawData = nuevaRawData;
                this.lblNombre.setText(this.nombre);
            }
        }

        public void setEstado(boolean estado) {
            this.encendido = estado;
            lblEstado.setText(encendido ? "ENCENDIDO" : "APAGADO");
            lblEstado.setForeground(encendido ? ACCENT_GREEN : ACCENT_RED);
            lblEstado.setBorder(BorderFactory.createLineBorder(encendido ? ACCENT_GREEN : ACCENT_RED, 1));
        }

        private void eliminarDispositivo() {
            usuarioActivo.getMisDispositivos().remove(rawData);
            gestor.guardarDatos();
            listaPanelesDispositivos.remove(this);
            panelDashboardTarjetas.remove(this);
            panelDashboardTarjetas.revalidate();
            panelDashboardTarjetas.repaint();
        }

        private void mostrarCalculadoraHogar() {
            int horasUso = 8, diasUso = 30;
            try {
                String h = JOptionPane.showInputDialog(this, "¿Cuántas horas al día se usa?", "8");
                if (h == null) return;
                horasUso = Integer.parseInt(h);
                
                String d = JOptionPane.showInputDialog(this, "¿Cuántos días al mes?", "30");
                if (d == null) return;
                diasUso = Integer.parseInt(d);
            } catch(Exception ex) { return; }

            double kWhMensual = (this.watts * horasUso * diasUso) / 1000.0;
            double costoTotal = kWhMensual * TARIFA_KWH;

            String msj = String.format(
                "<html><body style='width: 250px; color: white;'>" +
                "<h2>📊 Detalles: %s</h2>" +
                "<b>Potencia Nominal:</b> %d W<br>" +
                "<b>Tiempo de uso:</b> %d hrs/día por %d días<br><br>" +
                "<h3 style='color: #00d4ff;'>Consumo Mensual: %.2f kWh</h3>" +
                "<h3 style='color: #39ff14;'>Costo Estimado: $ %,.0f COP</h3>" +
                "</body></html>", 
                this.nombre, this.watts, horasUso, diasUso, kWhMensual, costoTotal
            );

            UIManager.put("OptionPane.background", BG_COLOR);
            UIManager.put("Panel.background", BG_COLOR);
            JOptionPane.showMessageDialog(this, msj, "Calculadora Hogar", JOptionPane.INFORMATION_MESSAGE);
        }

        private ImageIcon redimensionarIcono(String ruta, int ancho, int alto) {
            try {
                ImageIcon iconoOriginal = new ImageIcon(ruta);
                if (iconoOriginal.getIconWidth() > 0) { 
                    Image imgEscalada = iconoOriginal.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                    return new ImageIcon(imgEscalada);
                }
            } catch (Exception e) {
                System.err.println("No se pudo procesar la imagen: " + ruta);
            }
            return null;
        }
    }
}
