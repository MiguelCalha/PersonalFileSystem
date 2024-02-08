package pt.pa.adts;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Classe MAIN do PFS para executar a aplicação
 */
public class PFSStart extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("PFS");
		InputStream resource = PFSStart.class.getResourceAsStream("FileManager.png");
		if (resource != null) {
			Image icon = new Image(resource);
			primaryStage.getIcons().add(icon);
		}
		FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
		Parent root = loader.load();
		Controller controller = loader.getController();
		PFSView view = new PFSView();
		view.setPrimaryStage(primaryStage);
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Starts the JavaFX app.
	 * @param args the command line arguments.
	 */
	public static void run(String[] args) {
		launch(args);
	}
}
