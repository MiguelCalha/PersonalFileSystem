package pt.pa.adts;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Classe para obter os detalhes dos ficheiros
 */
	public class FileInfo {
		private final SimpleStringProperty nameProperty;
		private final SimpleStringProperty dateProperty;
		private final SimpleStringProperty sizeProperty;
		private final SimpleStringProperty lastModifiedProperty;
		private final SimpleIntegerProperty modificationCountProperty;
		private final SimpleStringProperty statusProperty;

		public FileInfo(File file, int modificationCount, boolean isLocked) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

			this.nameProperty = new SimpleStringProperty(file.getName());
			this.dateProperty = new SimpleStringProperty(dateFormat.format(new Date(file.lastModified())));
			this.sizeProperty = new SimpleStringProperty(String.valueOf(file.length()));
			this.lastModifiedProperty = new SimpleStringProperty(dateFormat.format(new Date(file.lastModified())));
			this.modificationCountProperty = new SimpleIntegerProperty(modificationCount);
			this.statusProperty = new SimpleStringProperty(isLocked ? "Locked" : "Unlocked");
		}

		public SimpleStringProperty nameProperty() { return nameProperty; }
		public SimpleStringProperty dateProperty() { return dateProperty; }
		public SimpleStringProperty sizeProperty() { return sizeProperty; }
		public SimpleStringProperty lastModifiedProperty() { return lastModifiedProperty; }
		public SimpleIntegerProperty modificationCountProperty() { return modificationCountProperty; }
		public SimpleStringProperty statusProperty() { return statusProperty; }

		public String getName() { return nameProperty.get(); }
		public String getDate() { return dateProperty.get(); }
		public String getSize() { return sizeProperty.get(); }
		public String getLastModified() { return lastModifiedProperty.get(); }
		public int getModificationCount() { return modificationCountProperty.get(); }
		public String getStatus() { return statusProperty.get(); }
	}

