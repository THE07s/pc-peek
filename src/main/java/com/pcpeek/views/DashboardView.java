package com.pcpeek.views;

import com.pcpeek.SystemMonitor;
import com.pcpeek.HardwareMonitor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxSizing;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;

@PageTitle("PC Peek Dashboard")
@Route("")
@Menu(order = 0, icon = "la la-chart-area")
public class DashboardView extends Main {

    private SystemMonitor systemMonitor;
    private HardwareMonitor hwMonitor;

    public DashboardView() {
        addClassName("dashboard-view");
        
        // Initialiser les moniteurs système
        initializeMonitors();

        Board board = new Board();
        
        // Première ligne : métriques principales
        board.addRow(
            createHighlight("Charge CPU", String.format("%.1f%%", getCpuLoad()), 0.0),
            createHighlight("Température CPU", String.format("%.1f°C", getCpuTemperature()), 0.0),
            createHighlight("Charge GPU", getGpuLoadString(), 0.0),
            createHighlight("Température GPU", getGpuTemperatureString(), 0.0)
        );
        
        // Deuxième ligne : graphique des charges CPU par cœur
        board.addRow(createCpuLoadChart());
        
        // Troisième ligne : informations détaillées et température
        board.addRow(createSystemInfo(), createTemperatureGauge());
        
        add(board);
    }

    private void initializeMonitors() {
        try {
            this.systemMonitor = new SystemMonitor();
            this.hwMonitor = new HardwareMonitor();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des moniteurs: " + e.getMessage());
        }
    }

    private Component createHighlight(String title, String value, Double percentage) {
        VaadinIcon icon = VaadinIcon.ARROW_UP;
        String prefix = "";
        String theme = "badge";

        if (percentage == 0) {
            icon = VaadinIcon.MINUS;
            theme = "badge contrast";
        } else if (percentage > 0) {
            icon = VaadinIcon.ARROW_UP;
            theme = "badge success";
            prefix = "+";
        } else {
            icon = VaadinIcon.ARROW_DOWN;
            theme = "badge error";
        }

        H2 h2 = new H2(title);
        h2.addClassNames(FontWeight.NORMAL, Margin.NONE, TextColor.SECONDARY, FontSize.XSMALL);

        Span span = new Span(value);
        span.addClassNames(FontWeight.SEMIBOLD, FontSize.XXXLARGE);

        Icon i = icon.create();
        i.addClassNames(BoxSizing.BORDER, Padding.XSMALL);

        Span badge = new Span(i, new Span(prefix + percentage.toString()));
        badge.getElement().getThemeList().add(theme);

        VerticalLayout layout = new VerticalLayout(h2, span, badge);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private Component createCpuLoadChart() {
        // Header
        HorizontalLayout header = createHeader("CPU : Charge & Température dans le temps", "Temps réel");

        // Chart
        Chart chart = new Chart(ChartType.LINE);
        Configuration conf = chart.getConfiguration();
        conf.getChart().setStyledMode(true);

        // Axe X : le temps (sera alimenté dynamiquement)
        XAxis xAxis = new XAxis();
        xAxis.setTitle("Temps");
        conf.addxAxis(xAxis);

        // Axe Y gauche : Charge CPU (%)
        YAxis yAxisLeft = new YAxis();
        yAxisLeft.setTitle("Charge CPU (%)");
        yAxisLeft.setMin(0);
        yAxisLeft.setMax(100);
        yAxisLeft.setOpposite(false);
        conf.addyAxis(yAxisLeft);

        // Axe Y droit : Température CPU (°C)
        YAxis yAxisRight = new YAxis();
        yAxisRight.setTitle("Température CPU (°C)");
        yAxisRight.setMin(0);
        yAxisRight.setMax(120);
        yAxisRight.setOpposite(true);
        conf.addyAxis(yAxisRight);

        // Série 1 : Charge CPU (sera alimentée dynamiquement)
        ListSeries cpuLoadSeries = new ListSeries("Charge CPU (%)");
        cpuLoadSeries.setyAxis(0); // Y gauche
        conf.addSeries(cpuLoadSeries);

        // Série 2 : Température CPU (sera alimentée dynamiquement)
        ListSeries cpuTempSeries = new ListSeries("Température CPU (°C)");
        cpuTempSeries.setyAxis(1); // Y droit
        conf.addSeries(cpuTempSeries);

        VerticalLayout layout = new VerticalLayout(header, chart);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private Component createSystemInfo() {
        HorizontalLayout header = createHeader("Informations Système", "Détails matériels");

        Grid<SystemInfoItem> grid = new Grid<>(SystemInfoItem.class, false);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        grid.addColumn(SystemInfoItem::getComponent).setHeader("Composant").setFlexGrow(1);
        grid.addColumn(SystemInfoItem::getValue).setHeader("Valeur").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);

        grid.setItems(
            new SystemInfoItem("Processeur", getCpuName()),
            new SystemInfoItem("Fréquence", String.format("%.0f MHz", getCpuFrequency())),
            new SystemInfoItem("Mémoire Totale", formatBytes(getTotalMemory())),
            new SystemInfoItem("Mémoire Disponible", formatBytes(getAvailableMemory())),
            new SystemInfoItem("Système", getOsName())
        );

        VerticalLayout layout = new VerticalLayout(header, grid);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private Component createTemperatureGauge() {
        HorizontalLayout header = createHeader("Température CPU", "Monitoring thermique");

        Chart chart = new Chart(ChartType.SOLIDGAUGE);
        Configuration conf = chart.getConfiguration();
        conf.getChart().setStyledMode(true);

        Pane pane = new Pane();
        pane.setStartAngle(-90);
        pane.setEndAngle(90);
        conf.addPane(pane);

        YAxis yAxis = new YAxis();
        yAxis.setMin(0);
        yAxis.setMax(100);
        yAxis.setTitle("°C");
        conf.addyAxis(yAxis);

        PlotOptionsSolidgauge plotOptions = new PlotOptionsSolidgauge();
        DataLabels dataLabels = new DataLabels();
        dataLabels.setFormat("{y}°C");
        plotOptions.setDataLabels(dataLabels);
        conf.addPlotOptions(plotOptions);

        ListSeries series = new ListSeries("Température", getCpuTemperature());
        conf.addSeries(series);

        VerticalLayout layout = new VerticalLayout(header, chart);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private HorizontalLayout createHeader(String title, String subtitle) {
        H2 h2 = new H2(title);
        h2.addClassNames(FontSize.XLARGE, Margin.NONE);

        Span span = new Span(subtitle);
        span.addClassNames(TextColor.SECONDARY, FontSize.XSMALL);

        VerticalLayout column = new VerticalLayout(h2, span);
        column.setPadding(false);
        column.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(column);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setSpacing(false);
        header.setWidthFull();
        return header;
    }

    // Méthodes d'accès aux données système avec gestion d'erreur
    private double getCpuLoad() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private double getMemoryUsage() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private double getCpuTemperature() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private int getCpuCores() {
        // À implémenter dynamiquement
        return 0;
    }

    private double[] getCpuLoadPerCore() {
        // À implémenter dynamiquement
        return new double[0];
    }

    private String getCpuName() {
        // À implémenter dynamiquement
        return "N/A";
    }

    private double getCpuFrequency() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private long getTotalMemory() {
        // À implémenter dynamiquement
        return 0L;
    }

    private long getAvailableMemory() {
        // À implémenter dynamiquement
        return 0L;
    }

    private double getGpuLoad() {
        // À implémenter dynamiquement
        return 0.0;
    }
    private String getGpuLoadString() {
        // À implémenter dynamiquement
        return "N/A";
    }
    private double getGpuTemperature() {
        // À implémenter dynamiquement
        return 0.0;
    }
    private String getGpuTemperatureString() {
        // À implémenter dynamiquement
        return "N/A";
    }

    private String getOsName() {
        // À implémenter dynamiquement
        return "N/A";
    }

    private String formatBytes(long bytes) {
        // À implémenter dynamiquement
        return String.valueOf(bytes);
    }

    // Classe interne pour les éléments d'information système
    public static class SystemInfoItem {
        private String component;
        private String value;

        public SystemInfoItem(String component, String value) {
            this.component = component;
            this.value = value;
        }

        public String getComponent() {
            return component;
        }

        public String getValue() {
            return value;
        }
    }
}
