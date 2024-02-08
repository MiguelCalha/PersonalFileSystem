package pt.pa.adts;

import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;

import java.io.File;

/**
 * Utilizada para renderizar elementos da tree
 */
public class FileTreeCell extends TreeCell<File> {


	/**
	 * Creates a new instance.
	 */
	public FileTreeCell() {

	}


	/**
	 * Método para atualizar um item
	 * @param file The new item for the cell.
	 * @param empty whether or not this cell represents data from the list. If it
	 *        is empty, then it does not represent any domain data, but is a cell
	 *        being used to render an "empty" row.
	 */
	@Override
	protected void updateItem(File file, boolean empty) {
		super.updateItem(file, empty);
		if (empty || file == null) {
			setText(null);
			setGraphic(null);
			setContextMenu(null);
		} else {
			setText(file.getName());
			setGraphic(getTreeItem().getGraphic());
			if (file.isDirectory() && getTreeItem() != null) {
				TreeItem<File> treeItem = getTreeItem();
				treeItem.getChildren().clear();
				File[] children = file.listFiles();
				if (children != null) {
					for (File child : children) {
						TreeItem<File> childTreeItem = new TreeItem<>(child);
						treeItem.getChildren().add(childTreeItem);
					}
				}
			}
		}
	}
}



