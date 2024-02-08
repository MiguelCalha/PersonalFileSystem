package pt.pa.adts;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

/**
 * Classe com os elementos gráficos do PFS
 */
public class PFSView {

    Controller controller;
    private Stage primaryStage;

    PFS pfs;
    /**
     * Método para mostrar erros
     * @param throwable the exception or error.
     */
    @FXML
    public void showError(Throwable throwable) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(primaryStage);
            alert.setTitle("Error");
            alert.setHeaderText("An error occured!");
            alert.setContentText(throwable.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String exceptionText = sw.toString();
            Label label = new Label("Stacktrace:");
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);
            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);
            alert.getDialogPane().setExpandableContent(expContent);
            alert.showAndWait();
        });
    }

    /**
     * Mostrar alertas
     * @param alertType
     * @param title
     * @param headerText
     * @param contentText
     */
    @FXML
    public void showAlert(Alert.AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    /**
     * Mostrar conteudo read only
     * @param title
     * @param header
     * @param content
     */
    @FXML
    public void showReadOnlyContentDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    /**
     * Método para mostrar uma caoixa de confirmação
     * @param title
     * @param headerText
     * @param contentText
     * @return
     */
    @FXML
    public boolean showConfirmation(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == ButtonType.OK;
    }

    /**
     * Mostrar input de texto
     * @param title
     * @param headerText
     * @param contentText
     * @param defaultValue
     * @return
     */
    @FXML
    public String showTextInputDialog(String title, String headerText, String contentText, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.initOwner(primaryStage);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Metodo para mostrar confirmacao Yes or No
     * @param title
     * @param headerText
     * @param contentText
     * @return
     */
    @FXML
    public boolean showYesNoConfirmation(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;


    }

    /**
     * Metodo que mostra o grafico
     * @param title
     * @param dataByMonth
     */
    @FXML
     void showChart(String title, Map<String, Integer> dataByMonth) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(1);
        yAxis.setUpperBound(50);
        yAxis.setTickUnit(1);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);
        dataByMonth.forEach((monthYear, count) -> {
            XYChart.Data<String, Number> data = new XYChart.Data<>(monthYear, count);
            series.getData().add(data);
        });
        chart.getData().add(series);
        chart.setCategoryGap(300);
        VBox vbox = new VBox(chart);
        Scene scene = new Scene(vbox);
        Stage chartStage = new Stage();
        chartStage.initModality(Modality.APPLICATION_MODAL);
        chartStage.setTitle(title);
        chartStage.setScene(scene);
        chartStage.show();
    }


    /**
     * MOVE TO PFS VIEW
     * @param primaryStage the primary stage.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }


    public void setController(Controller controller) {
        this.controller = controller;
    }

}
