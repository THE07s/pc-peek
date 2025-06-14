package com.pcpeek.views;

import com.pcpeek.SystemData;
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

    private SystemData systemData;

    public DashboardView() {
        addClassName("dashboard-view");
        // Initialiser les moniteurs système
        initializeMonitors();
        add(buildMainBoard());
    }

    private Board buildMainBoard() {
        Board board = new Board();

        // Colonne de gauche : image + infos système
        Component leftColumn = createDeviceInfoColumn();
        leftColumn.getElement().getStyle().remove("flex-basis");
        leftColumn.getElement().getStyle().set("minWidth", "180px").set("maxWidth", "450px");

        // Colonne de droite : highlights, graphique CPU, jauge température
        VerticalLayout rightColumn = new VerticalLayout();
        rightColumn.setPadding(false);
        rightColumn.setSpacing(false);
        rightColumn.setWidthFull();

        // Highlights (ligne de 4)
        HorizontalLayout highlights = new HorizontalLayout(
                createHighlight("Charge CPU", formatOrNA(getCPULoad(), "%.1f%%"), 0.0),
                createHighlight("Température CPU", formatOrNA(getCPUTemperature(), "%.1f°C"), 0.0),
                createHighlight("Charge GPU", formatOrNA(getGPULoad(), "%.1f%%"), 0.0),
                createHighlight("Température GPU", formatOrNA(getGPUTemperature(), "%.1f°C"), 0.0));
        highlights.setWidthFull();
        highlights.setSpacing(true);
        highlights.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        rightColumn.add(highlights);

        // Graphique CPU + RAM usage côte à côte
        HorizontalLayout cpuAndRamRow = new HorizontalLayout();
        cpuAndRamRow.setWidthFull();
        cpuAndRamRow.setSpacing(true);
        cpuAndRamRow.setPadding(false);
        cpuAndRamRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        cpuAndRamRow.add(createCpuLoadChart(), createRamUsageBlock());
        rightColumn.add(cpuAndRamRow);

        // Contrôle du volume par application (sous le bloc CPU+RAM)
        rightColumn.add(createVolumePerAppBlock());

        // Utilisation d'un HorizontalLayout pour avoir une colonne gauche moins large
        HorizontalLayout mainRow = new HorizontalLayout(leftColumn, rightColumn);
        mainRow.setWidthFull();
        mainRow.setSpacing(false);
        mainRow.setPadding(false);
        mainRow.setFlexGrow(0, leftColumn);
        mainRow.setFlexGrow(1, rightColumn);

        board.addRow(mainRow);
        return board;
    }

    // --- Méthodes de création de composants UI ---
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
        layout.getStyle().set("flex", "1 1 auto").set("min-width", "200px");
        return layout;
    }

    private Component createCpuLoadChart() {
        // Header
        HorizontalLayout header = createHeader("CPU Load & Temps", "Temps réel");

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
        chart.getElement().getStyle().set("width", "100%");

        VerticalLayout layout = new VerticalLayout(header, chart);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("flex", "1 1 auto").set("min-width", "400px");
        return layout;
    }

    // Ajout d'une colonne verticale avec image + infos système
    private Component createDeviceInfoColumn() {
        // Image placeholder SVG animé
        String imageSvg = getDeviceImageSvg();
        com.vaadin.flow.component.html.Image deviceImage = new com.vaadin.flow.component.html.Image(
                imageSvg, "Image appareil");
        deviceImage.setWidth("300px");
        deviceImage.getStyle().set("display", "block").set("margin", "0 auto 1rem auto").setWidth("100%");

        Component systemInfo = createSystemInfo();

        VerticalLayout layout = new VerticalLayout(deviceImage, systemInfo);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle().setWidth("640px").setMaxWidth("1000px").setMinWidth("440px");
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
                new SystemInfoItem("Processeur", getCPUName()),
                new SystemInfoItem("Fréquence", String.format("%.0f MHz", getCPUFrequency())),
                new SystemInfoItem("Mémoire Totale", formatBytes(getTotalMemory())),
                new SystemInfoItem("Mémoire Disponible", formatBytes(getAvailableMemory())),
                new SystemInfoItem("Système", getOSName()));

        VerticalLayout layout = new VerticalLayout(header, grid);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    // Bloc RAM usage : Active, Résidente, Compressée sous forme de pie chart
    private Component createRamUsageBlock() {
        HorizontalLayout header = createHeader("RAM Usage", "Active, résidente et compressée");

        Chart chart = new Chart(ChartType.PIE);
        Configuration conf = chart.getConfiguration();
        conf.setTitle("");
        conf.getChart().setStyledMode(true);

        DataSeries series = new DataSeries();
        series.add(new DataSeriesItem("Active", getRAMActive()));
        series.add(new DataSeriesItem("Résidente", getRAMResident()));
        series.add(new DataSeriesItem("Compressée", getRAMCompressed()));
        conf.setSeries(series);

        PlotOptionsPie options = new PlotOptionsPie();
        options.setDataLabels(new DataLabels(true));
        conf.setPlotOptions(options);

        VerticalLayout layout = new VerticalLayout(header, chart);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getStyle().set("flex", "1 1 auto").set("min-width", "300px");
        return layout;
    }

    // Bloc de contrôle du volume par application
    private Component createVolumePerAppBlock() {
        HorizontalLayout header = createHeader("Volume par application", "Contrôle individuel");

        // Placeholder SVG animé pour le volume par application
        String volumeSvg = getVolumeControlSvg();
        com.vaadin.flow.component.html.Image volumePlaceholder = new com.vaadin.flow.component.html.Image(
                volumeSvg, "Volume control placeholder");
        volumePlaceholder.setWidth("100%");
        volumePlaceholder.getStyle().set("display", "block").set("margin", "1rem auto").setFlexGrow("1");

        VerticalLayout layout = new VerticalLayout(header, volumePlaceholder);
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

    // --- Méthodes utilitaires ---
    private void initializeMonitors() {
        try {
            this.systemData = new SystemData();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du système de données: " + e.getMessage());
        }
    }

    private String formatBytes(long bytes) {
        // À implémenter dynamiquement
        return String.valueOf(bytes);
    }

    /**
     * Formate une valeur numérique ou retourne "N/A" si la valeur est nulle ou
     * négative.
     */
    private String formatOrNA(double value, String format) {
        if (value > 0.0) {
            return String.format(format, value);
        } else {
            return "N/A";
        }
    }

    // --- Classes internes ---
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

    // --- Getters système ---
    private double getCPULoad() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private double getCPUTemperature() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private String getCPUName() {
        // À implémenter dynamiquement
        return "N/A";
    }

    private double getCPUFrequency() {
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

    private double getGPULoad() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private double getGPUTemperature() {
        // À implémenter dynamiquement
        return 0.0;
    }

    private String getOSName() {
        // À implémenter dynamiquement
        return "N/A";
    }

    // Getters RAM à implémenter dynamiquement
    private long getRAMActive() {
        // À implémenter dynamiquement
        return 0L;
    }

    private long getRAMResident() {
        // À implémenter dynamiquement
        return 0L;
    }

    private long getRAMCompressed() {
        // À implémenter dynamiquement
        return 0L;
    }

    // Méthode utilitaire pour créer un placeholder SVG animé
    private String getDeviceImageSvg() {
        return "data:image/svg+xml;base64," + java.util.Base64.getEncoder().encodeToString(
                ("<?xml version='1.0' encoding='UTF-8'?>" +
                        "<svg width='300' height='300' viewBox='0 0 300 300' xmlns='http://www.w3.org/2000/svg'>" +
                        "<defs>" +
                        "<style>" +
                        ".loading { animation: pulse 2s ease-in-out infinite; }" +
                        "@keyframes pulse { 0%, 100% { opacity: 0.4; } 50% { opacity: 1; } }" +
                        ".rotate { animation: rotate 2s linear infinite; transform-origin: 150px 140px; }" +
                        "@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }" +
                        "</style>" +
                        "</defs>" +
                        "<rect width='300' height='300' fill='#f8f9fa' stroke='#e9ecef' stroke-width='2' rx='10'/>" +
                        "<g transform='translate(150,150)'>" +
                        "<!-- Écran d'ordinateur -->" +
                        "<rect x='-80' y='-60' width='160' height='100' fill='#343a40' rx='8' class='loading'/>" +
                        "<rect x='-70' y='-50' width='140' height='80' fill='#495057' rx='4'/>" +
                        "<rect x='-60' y='-40' width='120' height='60' fill='#6c757d'/>" +
                        "<!-- Base -->" +
                        "<rect x='-15' y='40' width='30' height='20' fill='#343a40'/>" +
                        "<rect x='-40' y='60' width='80' height='8' fill='#343a40' rx='4'/>" +
                        "</g>" +
                        "<!-- Icône loading au centre de l'écran -->" +
                        "<circle cx='150' cy='140' r='17' fill='none' stroke='#007bff' stroke-width='3' opacity='0.3'/>"
                        +
                        "<circle cx='150' cy='140' r='17' fill='none' stroke='#007bff' stroke-width='3' " +
                        "stroke-dasharray='47.12' stroke-dashoffset='35.34' class='rotate'/>" +
                        "<text x='150' y='270' text-anchor='middle' fill='#6c757d' font-family='Roboto, sans-serif' font-size='14'>"
                        +
                        "Future utilisation de l'API Icecat..." +
                        "</text>" +
                        "<text x='150' y='80' text-anchor='middle' fill='#6c757d' font-family='Roboto, sans-serif' font-size='14'>"
                        +
                        "Image de l'ordinateur" +
                        "</text>" +
                        "</svg>").getBytes());
    }

    // Méthode utilitaire pour créer un placeholder SVG animé pour le volume par
    // application
    private String getVolumeControlSvg() {
        return "data:image/svg+xml;base64," + java.util.Base64.getEncoder().encodeToString(
                ("<?xml version='1.0' encoding='UTF-8'?>" +
                        "<svg width='100%' height='200' viewBox='0 0 600 200' xmlns='http://www.w3.org/2000/svg'>" +
                        "<defs>" +
                        "<style>" +
                        ".fade { animation: fadeInOut 3s ease-in-out infinite; }" +
                        "@keyframes fadeInOut { 0%, 100% { opacity: 0.4; } 50% { opacity: 1; } }" +
                        ".slide { animation: slideVolume 4s ease-in-out infinite; }" +
                        "@keyframes slideVolume { 0%, 100% { transform: translateX(0); } 50% { transform: translateX(10px); } }"
                        +
                        ".bounce { animation: bounce 2s ease-in-out infinite; }" +
                        "@keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-3px); } }"
                        +
                        "</style>" +
                        "</defs>" +
                        "<rect width='100%' height='200' fill='#f8f9fa' stroke='#e9ecef' stroke-width='1' rx='8'/>" +

                        "<!-- Application 1: Spotify -->" +
                        "<g transform='translate(50, 30)'>" +
                        "<circle cx='15' cy='15' r='12' fill='#1DB954' class='bounce'/>" +
                        "<path d='M8 10 Q15 6 22 10 Q15 14 8 18 Q15 14 22 18' stroke='white' stroke-width='2' fill='none'/>"
                        +
                        "<text x='40' y='20' fill='#343a40' font-family='Roboto, sans-serif' font-size='14'>Spotify</text>"
                        +
                        "<rect x='120' y='10' width='200' height='4' fill='#e9ecef' rx='2'/>" +
                        "<rect x='120' y='10' width='140' height='4' fill='#1DB954' rx='2' class='slide'/>" +
                        "<circle cx='260' cy='12' r='6' fill='#1DB954' class='slide'/>" +
                        "<text x='480' y='20' fill='#6c757d' font-family='Roboto, sans-serif' font-size='12'>70%</text>"
                        +
                        "</g>" +

                        "<!-- Application 2: Chrome -->" +
                        "<g transform='translate(50, 80)'>" +
                        "<circle cx='15' cy='15' r='12' fill='#4285F4' class='bounce'/>" +
                        "<circle cx='15' cy='15' r='8' fill='white'/>" +
                        "<circle cx='15' cy='15' r='5' fill='#4285F4'/>" +
                        "<text x='40' y='20' fill='#343a40' font-family='Roboto, sans-serif' font-size='14'>Chrome</text>"
                        +
                        "<rect x='120' y='10' width='200' height='4' fill='#e9ecef' rx='2'/>" +
                        "<rect x='120' y='10' width='80' height='4' fill='#4285F4' rx='2' class='slide'/>" +
                        "<circle cx='200' cy='12' r='6' fill='#4285F4' class='slide'/>" +
                        "<text x='480' y='20' fill='#6c757d' font-family='Roboto, sans-serif' font-size='12'>40%</text>"
                        +
                        "</g>" +

                        "<!-- Application 3: Teams -->" +
                        "<g transform='translate(50, 130)'>" +
                        "<rect x='3' y='3' width='24' height='24' fill='#6264A7' rx='4' class='bounce'/>" +
                        "<path d='M12 8 L20 8 L20 16 L12 16 Z M8 12 L12 12 L12 20 L8 20 Z' fill='white'/>" +
                        "<text x='40' y='20' fill='#343a40' font-family='Roboto, sans-serif' font-size='14'>Teams</text>"
                        +
                        "<rect x='120' y='10' width='200' height='4' fill='#e9ecef' rx='2'/>" +
                        "<rect x='120' y='10' width='180' height='4' fill='#6264A7' rx='2' class='slide'/>" +
                        "<circle cx='300' cy='12' r='6' fill='#6264A7' class='slide'/>" +
                        "<text x='480' y='20' fill='#6c757d' font-family='Roboto, sans-serif' font-size='12'>90%</text>"
                        +
                        "</g>" +

                        "<!-- Texte d'indication -->" +
                        "<text x='300' y='185' text-anchor='middle' fill='#6c757d' font-family='Roboto, sans-serif' font-size='13' class='fade'>"
                        +
                        "Contrôles de volume individuels - À implémenter" +
                        "</text>" +
                        "</svg>").getBytes());
    }
}
