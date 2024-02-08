package pt.pa.adts;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Controller da aplicação
 */
public class Controller implements Initializable {
	@FXML
	private TableView<FileInfo> tableView;
	@FXML
	private TreeView<File> treeView;
	private Stage primaryStage;
	private static final Logger logger = AppLogger.getLogger();
	private PFS pfs;
	private Map<File, TreeItem<File>> fileToTreeItemMap = new HashMap<>();
	private Deque<UndoableCommand> undoStack = new ArrayDeque<>();

	private PFSView view;

	private Metrics metrics;


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.view = new PFSView();
		this.metrics = new Metrics();
		pfs = new PFS();
		this.view.setPrimaryStage(primaryStage);

		setupTreeViewSelectionListener();
		TableColumn<FileInfo, String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableView.getColumns().add(nameColumn);

		TableColumn<FileInfo, String> dateColumn = new TableColumn<>("Creation Date");
		dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
		tableView.getColumns().add(dateColumn);

		TableColumn<FileInfo, String> sizeColumn = new TableColumn<>("Size");
		sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
		tableView.getColumns().add(sizeColumn);


		TableColumn<FileInfo, String> lastModifiedColumn = new TableColumn<>("Last Modified Date");
		lastModifiedColumn.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
		tableView.getColumns().add(lastModifiedColumn);


		TableColumn<FileInfo, Number> modificationCountColumn = new TableColumn<>("Modification Count");
		modificationCountColumn.setCellValueFactory(new PropertyValueFactory<>("modificationCount"));
		tableView.getColumns().add(modificationCountColumn);


		TableColumn<FileInfo, String> statusColumn = new TableColumn<>("Status");
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		tableView.getColumns().add(statusColumn);

		treeView.setCellFactory(param -> new FileTreeCell());

		setupTreeViewSelectionListener();

		treeView.setCellFactory(tv -> {
			TreeCell<File> cell = new TreeCell<File>() {
				@Override
				protected void updateItem(File item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setGraphic(null);
						setContextMenu(null);
					} else {
						setText(item.getName());

						ContextMenu contextMenu = new ContextMenu();

						MenuItem createFile = new MenuItem("Create New File");
						createFile.setOnAction(event -> createNewFile(getTreeItem()));


						MenuItem createFolder = new MenuItem("Create New Folder");
						createFolder.setOnAction(event -> createNewFolder(getTreeItem()));

						MenuItem editFile = new MenuItem("Edit File");
						editFile.setOnAction(event -> editFile(getTreeItem()));

						MenuItem renameItem = new MenuItem("Rename");
						renameItem.setOnAction(event -> handleRename(event));

						MenuItem removeItem = new MenuItem("Remove");
						removeItem.setOnAction(event -> removeFile());

						MenuItem move = new MenuItem("Move");
						move.setOnAction(event -> handleMove(event));

						MenuItem zip = new MenuItem("Zip");
						zip.setOnAction(event -> zipFileOrFolder(getTreeItem()));

						MenuItem copy = new MenuItem("Copy");
						copy.setOnAction(event -> copyFileOrFolder(getTreeItem()));

						MenuItem viewFile = new MenuItem("View File");
						viewFile.setOnAction(event -> viewFile(getTreeItem()));

						MenuItem searchItem = new MenuItem("Search");
						searchItem.setOnAction(event -> searchDirectories());


						contextMenu.getItems().addAll(createFile, createFolder, editFile, renameItem, removeItem, move, copy, viewFile, zip, searchItem);

						setContextMenu(contextMenu);
					}
				}
			};
			return cell;
		});
	}

	/**
	 * Método helper para contar as modificações dos ficheiros
	 */
	private void setupTreeViewSelectionListener() {
		treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			tableView.getItems().clear();
			if (newVal != null) {
				File selectedFile = newVal.getValue();
				Position<File> filePosition = pfs.findPositionInTreeLinked(selectedFile.getParentFile(), selectedFile.getName());
				boolean isLocked = filePosition != null && pfs.isLocked(filePosition);
				File fileForModCount = (filePosition != null) ? filePosition.element() : null;

				int modificationCount = (fileForModCount != null) ? metrics.getModificationCount(fileForModCount) : 0;

				FileInfo fileInfo = new FileInfo(selectedFile, modificationCount, isLocked);
				tableView.getItems().add(fileInfo);
			}
		});
	}


	/**
	 * Método para o About da interface
	 * @param event
	 */
	@FXML
	void handleAbout(ActionEvent event) {
		view.showAlert(AlertType.INFORMATION, "Sobre", "Sobre PFS",
				"Programa que utiliza ADT Tree realizado no âmbito da cadeira de Programação Avançada");
	}

	/**
	 * Método para sair da aplicação
	 * @param event
	 */

	@FXML
	void handleExit(ActionEvent event) {
		Platform.exit();
	}

	/**
	 * Método para abrir uma pasta do computador e inserir na TreeLinked
	 * @param event
	 */
	@FXML
	void handleOpen(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Open Directory");
		File dir = directoryChooser.showDialog(primaryStage);

		if (dir != null) {
			pfs.treeLinked = new TreeLinked<>();

			pfs.addContentsToTreeLinked(dir, pfs.treeLinked, null);

			TreeItem<File> rootTreeItem = new TreeItem<>(dir);
			treeView.setRoot(rootTreeItem);

			populateTreeView(dir, rootTreeItem);

			rootTreeItem.setExpanded(true);
			logger.info("Opened directory and inserted files into TreeView and TreeLinked");
		}

		pfs.printTreeLinked();
	}


	/**
	 * Método para popular a Treeview
	 * @param directory
	 * @param treeItem
	 */
	private void populateTreeView(File directory, TreeItem<File> treeItem) {
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				TreeItem<File> childItem = new TreeItem<>(file);
				treeItem.getChildren().add(childItem);
				if (file.isDirectory()) {
					populateTreeView(file, childItem);
				}
				fileToTreeItemMap.put(file, childItem);
			}
		}
	}

	/**
	 * Método para criar um item na arvore
	 * @param file
	 * @return
	 */
	private TreeItem<File> createTreeItem(File file) {
		TreeItem<File> treeItem = new TreeItem<>(file) {
			private boolean childrenLoaded = false;

			@Override
			public boolean isLeaf() {
				return getValue().isFile();
			}

			@Override
			public ObservableList<TreeItem<File>> getChildren() {
				if (!childrenLoaded && !isLeaf()) {
					childrenLoaded = true;
					File[] files = getValue().listFiles();
					if (files != null) {
						for (File f : files) {
							super.getChildren().add(createTreeItem(f));
						}
					}
				}
				return super.getChildren();
			}
		};


		return treeItem;
	}

	/**
	 * Método que cria novas pastas
	 * @param parentItem
	 */

	private void createNewFolder(TreeItem<File> parentItem) {
		if (parentItem != null && parentItem.getValue().isDirectory()) {
			String folderName = view.showTextInputDialog("Create New Folder", "Enter folder name:", "Folder name:", "");
			if (folderName != null && !folderName.trim().isEmpty()) {
				File parentFolder = parentItem.getValue();

				if (parentFolder.exists() && parentFolder.isDirectory()) {
					Position<File> parentPosition = pfs.findFolderPositionInTreeLinked(parentFolder);

					if (parentPosition != null) {
						try {
							pfs.createFolder(parentPosition, folderName);

							File newFolderFile = new File(parentFolder, folderName);
							TreeItem<File> newFolderItem = new TreeItem<>(newFolderFile);
							parentItem.getChildren().add(newFolderItem);
							parentItem.setExpanded(true);
							fileToTreeItemMap.put(newFolderFile, newFolderItem);
							CreateFolderCommand command = new CreateFolderCommand(newFolderFile);
							undoStack.push(command);
						} catch (IllegalArgumentException e) {
							view.showAlert(AlertType.ERROR, "Error", "Cannot Create Folder", e.getMessage());
						}
					} else {
						view.showAlert(AlertType.ERROR, "Error", "Cannot Create Folder", "Parent folder not found in TreeLinked.");
					}
				} else {
					view.showAlert(AlertType.ERROR, "Error", "Cannot Create Folder", "Parent directory does not exist or is not a folder.");
				}
			}
		}
	}







	/**
	 * Método que cria novos ficheiros
	 * @param parentItem
	 */
	private void createNewFile(TreeItem<File> parentItem) {
		if (parentItem != null && parentItem.getValue().isDirectory()) {
			String fileName = view.showTextInputDialog("Create New File", "Enter file name:", "File name:", "");
			if (fileName != null && !fileName.trim().isEmpty()) {
				File parentFolder = parentItem.getValue();
				File newFile = new File(parentFolder, fileName);
				if (newFile.exists()) {
					view.showAlert(AlertType.ERROR, "Error", "Cannot Create File", "File with the same name already exists.");
				} else {
					try {
						if (newFile.createNewFile()) {
							boolean isLocked = view.showYesNoConfirmation("Lock File?", "Do you want to lock the file?", "Lock File");
							Position<File> parentPosition = pfs.findParentPositionForFile(parentFolder);
							if (parentPosition != null) {
								pfs.createFile(parentPosition, fileName, isLocked, "");

								TreeItem<File> newFileItem = new TreeItem<>(newFile);
								parentItem.getChildren().add(newFileItem);
								parentItem.setExpanded(true);
								fileToTreeItemMap.put(newFileItem.getValue(), newFileItem);

								if (isLocked) {
									pfs.lockFile(parentPosition);
								} else {
									pfs.unlockFile(parentPosition);
								}
								CreateFileCommand command = new CreateFileCommand(newFile);
								undoStack.push(command);
							} else {
								view.showAlert(AlertType.ERROR, "Error", "Folder Not Found", "Parent folder not found in TreeLinked.");
							}
						} else {
							view.showAlert(AlertType.ERROR, "Error", "Cannot Create File", "File creation failed.");
						}
					} catch (IOException e) {
						view.showError(e);
					}
				}
			}
		}
	}






	/**
	 * Método que edita ficheiros
	 * @param selectedFileItem
	 */
	private void editFile(TreeItem<File> selectedFileItem) {
		if (selectedFileItem != null && !selectedFileItem.getValue().isDirectory()) {
			File selectedFile = selectedFileItem.getValue();
			String filePath = selectedFile.getAbsolutePath();
			Position<File> filePosition = pfs.findPositionInTreeLinked(selectedFile.getParentFile(), selectedFile.getName());
			if (filePosition != null && pfs.isLocked(filePosition)) {
				view.showAlert(AlertType.ERROR, "Error", "File is Locked", "The selected file is locked and cannot be edited.");
			} else {
				try {
					String originalContent = getContentOfFile(selectedFile);
					ProcessBuilder processBuilder = new ProcessBuilder("notepad.exe", filePath);
					Process process = processBuilder.start();
					int exitCode = process.waitFor();

					if (exitCode == 0) {
						String updatedContent = getContentOfFile(selectedFile);
						if (!originalContent.equals(updatedContent)) {
							EditFileCommand editCommand = new EditFileCommand(selectedFile, originalContent, updatedContent);
							undoStack.push(editCommand);
						}
					} else {
						view.showAlert(AlertType.ERROR, "Error", "Notepad Error", "Notepad encountered an issue while editing the file.");
					}
				} catch (IOException | InterruptedException e) {
					view.showError(e);
				}
			}
		}
	}


	/**
	 * Retorna os conteúdos de um ficheiro
	 * @param file
	 * @return
	 */
	private String getContentOfFile(File file) {
		StringBuilder content = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append(System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return content.toString();
	}



	/**
	 * Método que renomeia ficheiros/pastas
	 * @param event
	 */
	@FXML
	private void handleRename(ActionEvent event) {
		TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
		if (selectedItem != null) {
			String currentName = selectedItem.getValue().getName();
			String newName = view.showTextInputDialog("Rename", "Enter new name:", "New name:", currentName);
			if (newName != null && !newName.trim().isEmpty() && !newName.equals(currentName)) {
				File originalFile = selectedItem.getValue();
				File newFile = new File(originalFile.getParentFile(), newName);

				if (!newFile.exists()) {
					try {
						boolean renameSuccessful = originalFile.renameTo(newFile);
						if (renameSuccessful) {
							selectedItem.setValue(newFile);
							fileToTreeItemMap.put(newFile, selectedItem);
							RenameCommand command = new RenameCommand(originalFile, newFile);
							undoStack.push(command);
							Position<File> elementPosition = pfs.findPositionInTreeLinked(originalFile.getParentFile(), currentName);
							if (elementPosition != null) {
								pfs.renameElement(elementPosition, newName);
							}
						} else {
							view.showAlert(AlertType.ERROR, "Error", "Rename Failed", "Could not rename the file on disk.");
						}
					} catch (Exception e) {
						view.showAlert(AlertType.ERROR, "Error", "Exception", "An error occurred: " + e.getMessage());
					}
				} else {
					view.showAlert(AlertType.ERROR, "Error", "Cannot Rename", "A file with that name already exists.");
				}
			}
		}
	}



	/**
	 * Método que remove ficheiros / Pastas
	 */
	private void removeFile() {
		TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();

		if (selectedItem != null) {
			File selectedFile = selectedItem.getValue();
			boolean confirmed = view.showConfirmation("Confirm Removal", "Confirm File/Folder Removal",
					"Are you sure you want to remove: " + selectedFile.getName() + "?");
			if (confirmed) {
				if (selectedFile.isDirectory()) {
					pfs.deleteElement(pfs.findPositionInTreeLinked(selectedFile.getParentFile(), selectedFile.getName()));
					if (selectedFile.exists()) {
						deleteFolder(selectedFile);
					}
				} else {
					pfs.deleteElement(pfs.findPositionInTreeLinked(selectedFile.getParentFile(), selectedFile.getName()));
					DeleteFileCommand deleteFileCommand = new DeleteFileCommand(selectedFile, selectedFile.getParentFile(), false);
					undoStack.push(deleteFileCommand);
					if (selectedFile.exists()) {
						selectedFile.delete();
					}
				}

				selectedItem.getParent().getChildren().remove(selectedItem);

			}
		}
	}

	/**
	 * Método para elimiar uma  pasta
	 * @param folder
	 */
	private void deleteFolder(File folder) {
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			if (files != null) {
				for (File file : files) {
					deleteFolder(file);
				}
			}
		}
		folder.delete();
	}


	/**
	 * Método para mover ficheiros/diretorias
	 * @param event
	 */
	@FXML
	void handleMove(ActionEvent event) {
		TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();

		if (selectedItem != null) {
			File sourceFile = selectedItem.getValue();
			TreeItem<File> originalParentItem = selectedItem.getParent();

			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select Destination Folder");
			File destinationFolder = directoryChooser.showDialog(primaryStage);

			if (destinationFolder != null) {
				if (!sourceFile.exists()) {
					view.showAlert(AlertType.ERROR, "Error", "File Not Found", "The selected file does not exist.");
					return;
				}
				if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
					view.showAlert(AlertType.ERROR, "Error", "Invalid Destination", "The selected destination is not a valid folder.");
					return;
				}
				File destinationFile = new File(destinationFolder, sourceFile.getName());
				if (destinationFile.exists()) {
					view.showAlert(AlertType.ERROR, "Error", "File Already Exists", "A file with the same name already exists in the destination folder.");
					return;
				}

				Position<File> sourcePosition = pfs.findPositionInTreeLinkedForMove(pfs.treeLinked, sourceFile, destinationFolder);
				Position<File> destinationPosition = pfs.findPositionInTreeLinkedForMove(pfs.treeLinked, destinationFolder, sourceFile);

				if (sourcePosition != null && destinationPosition != null) {
					pfs.moveElement(sourcePosition, destinationPosition);
					File originalFile = new File(sourceFile.getAbsolutePath()); // Copy of the original file path

					TreeItem<File> newParent = fileToTreeItemMap.get(destinationFolder);
					if (newParent == null) {
						newParent = new TreeItem<>(destinationFolder);
						treeView.getRoot().getChildren().add(newParent);
						fileToTreeItemMap.put(destinationFolder, newParent);
					}
					selectedItem.getParent().getChildren().remove(selectedItem);
					newParent.getChildren().add(selectedItem);

					MoveFileCommand moveCommand = new MoveFileCommand(originalFile, destinationFile, originalParentItem, newParent);
					undoStack.push(moveCommand);
				} else {
					view.showAlert(AlertType.ERROR, "Error", "Cannot Move File", "Failed to locate source or destination in the tree-linked structure.");
				}
			}
		}
	}


	/**
	 * Método para copuar ficheiro ou pasta
	 * @param sourceItem
	 */
	private void copyFileOrFolder(TreeItem<File> sourceItem) {
		if (sourceItem != null) {
			File sourceFile = sourceItem.getValue();
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select Destination Folder");
			File destinationFolder = directoryChooser.showDialog(primaryStage);

			if (destinationFolder != null) {
				try {
					if (sourceFile.getAbsolutePath().equals(destinationFolder.getAbsolutePath())) {
						view.showAlert(AlertType.ERROR, "Error", "Invalid Destination", "Source and destination cannot be the same.");
						return;
					}
					File newDestination = new File(destinationFolder, sourceFile.getName());
					if (newDestination.exists()) {
						view.showAlert(AlertType.ERROR, "Error", "File Already Exists", "A file with the same name already exists in the destination folder.");
						return;
					}
					Position<File> sourcePosition = pfs.findPositionInTreeLinked(sourceFile.getParentFile(), sourceFile.getName());
					Position<File> destinationPosition = pfs.findFolderPositionInTreeLinked(destinationFolder);

					if (sourcePosition != null && destinationPosition != null) {
						pfs.copyElement(sourcePosition, destinationPosition);
						copyFileOrFolderPhysically(sourceFile, newDestination);
						TreeItem<File> newDestinationItem = new TreeItem<>(newDestination);
						TreeItem<File> parentItem = fileToTreeItemMap.get(destinationFolder);
						if (parentItem != null) {
							parentItem.getChildren().add(newDestinationItem);
							parentItem.setExpanded(true);
							fileToTreeItemMap.put(newDestination, newDestinationItem);
						}

						CopyFileCommand copyCommand = new CopyFileCommand(sourceFile, newDestination);
						undoStack.push(copyCommand);

					} else {
						view.showAlert(AlertType.ERROR, "Error", "Cannot Copy File or Folder", "Failed to locate source or destination in the tree-linked structure.");
					}
				} catch (IOException e) {
					view.showError(e);
				}
			}
		}
	}


	/**
	 * Método para copiar fisicamente
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	private void copyFileOrFolderPhysically(File source, File destination) throws IOException {
		if (source.isDirectory()) {
			destination.mkdir();
			File[] files = source.listFiles();
			if (files != null) {
				for (File file : files) {
					copyFileOrFolderPhysically(file, new File(destination, file.getName()));
				}
			}
		} else {
			Files.copy(source.toPath(), destination.toPath());
		}
	}


	/**
	 * Método que visualiza o conteúdo de ficheiros
	 * @param selectedFileItem
	 */
	private void viewFile(TreeItem<File> selectedFileItem) {
		if (selectedFileItem != null && !selectedFileItem.getValue().isDirectory()) {
			File selectedFile = selectedFileItem.getValue();
			Position<File> filePosition = pfs.findPositionInTreeLinked(selectedFile.getParentFile(), selectedFile.getName());
			if (filePosition != null && pfs.isLocked(filePosition)) {
				view.showAlert(AlertType.ERROR, "Error", "File is Locked", "The selected file is locked and cannot be viewed.");
			} else {
				String fileContent = getContentOfFile(selectedFile);
				view.showReadOnlyContentDialog("View File", selectedFile.getName(), fileContent);
			}
		}
	}


	/**
	 * Method to zip the selected file or folder.
	 * @param selectedItem The selected item in the TreeView.
	 */
	private void zipFileOrFolder(TreeItem<File> selectedItem) {
		if (selectedItem != null) {
			File sourceFile = selectedItem.getValue();
			String zipFileName = sourceFile.getAbsolutePath();
			if (sourceFile.isDirectory()) {
				zipFileName += ".zip";
			} else {
				zipFileName = zipFileName.substring(0, zipFileName.lastIndexOf('.')) + ".zip";
			}
			try {
				// Perform the zipping operation
				zip(sourceFile, zipFileName);

				view.showAlert(AlertType.INFORMATION, "Success", "Zip Created", "The zip file was created successfully: " + zipFileName);
			} catch (IOException e) {
				view.showError(e);
			}
		}
	}


	/**
	 * Recursive method to zip a file or folder.
	 * @param fileToZip The file or folder to zip.
	 * @param zipFileName The name of the resulting zip file.
	 * @throws IOException If an I/O error occurs.
	 */
	private void zip(File fileToZip, String zipFileName) throws IOException {
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFileName))) {
			zipFile(fileToZip, fileToZip.getName(), zipOut);
		}
	}

	/**
	 * Helper method to add files to the ZipOutputStream.
	 * @param fileToZip The file to zip.
	 * @param fileName The name of the file in the zip.
	 * @param zipOut The ZipOutputStream.
	 * @throws IOException If an I/O error occurs.
	 */
	private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		try (FileInputStream fis = new FileInputStream(fileToZip)) {
			ZipEntry zipEntry = new ZipEntry(fileName);
			zipOut.putNextEntry(zipEntry);
			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zipOut.write(bytes, 0, length);
			}
		}
	}

	/**
	 * Método para procura de diretorias
	 */
	private void searchDirectories() {
		String searchTerm = view.showTextInputDialog("Search Directories", "Enter directory name to search:", "Directory Name:", "");
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			searchAndHighlight(searchTerm);
		}
	}


	/**
	 * Método para procura de diretorias
	 * @param searchTerm
	 */
	private void searchAndHighlight(String searchTerm) {
		boolean found = false;
		for (Position<File> position : pfs.treeLinked.positions()) {
			File file = position.element();
			if (file.isDirectory() && file.getName().contains(searchTerm)) {
				TreeItem<File> treeItem = fileToTreeItemMap.get(file);
				if (treeItem != null) {
					treeView.getSelectionModel().select(treeItem);
					treeItem.setExpanded(true);
					found = true;
					break;
				}
			}
		}

		if (!found) {
			view.showAlert(AlertType.INFORMATION, "Search", "No Results", "No directories found with name: " + searchTerm);
		}
	}

	/**
	 * Método para update table view
	 * @param directory
	 */
	private void updateTableView(File directory) {
		tableView.getItems().clear();
		if (directory != null && directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					Position<File> filePosition = pfs.findPositionInTreeLinked(directory, file.getName());
					int modificationCount = (filePosition != null) ? metrics.getModificationCount(file) : 0;
					boolean isLocked = filePosition != null && pfs.isLocked(filePosition);

					FileInfo fileInfo = new FileInfo(file, modificationCount, isLocked);
					tableView.getItems().add(fileInfo);
				}
			}
		}
	}


	/**
	 * Rfresh treeView
	 */
	private void refreshTreeView() {
		File rootDirectory = pfs.getRootOfTreeLinked();
		if (rootDirectory != null) {
			TreeItem<File> rootItem = new TreeItem<>(rootDirectory);
			treeView.setRoot(rootItem);
			populateTreeView(rootDirectory, rootItem);
			rootItem.setExpanded(true);
		} else {
			treeView.setRoot(null);
		}
	}

	/**
	 * Método para dar refresh à table view
	 */
	private void refreshTableView() {
		TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
		if (selectedItem != null) {
			updateTableView(selectedItem.getValue());
		} else {
			tableView.getItems().clear();
		}
	}




	/**
	 * Método para getStage
	 * @return
	 */
	public Window getStage() {
		return null;
	}


	/**
	 * Método para o button de undo
	 * @param event
	 */
	@FXML
	private void handleUndo(ActionEvent event) {
		if (!undoStack.isEmpty()) {
			try {
				UndoableCommand command = undoStack.pop();
				command.undo();
				refreshTreeView();
				refreshTableView();
			} catch (RuntimeException e) {
				view.showAlert(AlertType.ERROR, "Error", "Undo Failed", e.getMessage());
			}
		} else {
			view.showAlert(AlertType.INFORMATION, "Undo", "No Actions", "No more actions to undo.");
		}
		logger.info("UNDO CALLED");

	}


	/**
	 * Método das listas
	 * @param event
	 */
	@FXML
	void handleLists(ActionEvent event) {
		Dialog<Void> dialog = new Dialog<>();
		dialog.initOwner(primaryStage);
		dialog.setTitle("Metrics Information");
		Position<File> currentDirectoryPosition = pfs.treeLinked.root();
		HBox horizontalLayout = new HBox(10);
		ListView<File> descendantsList = new ListView<>(FXCollections.observableArrayList(metrics.getDirectDescendants(currentDirectoryPosition, FileType.BOTH)));
		ListView<Map.Entry<File, Integer>> filesAlteredList = new ListView<>(FXCollections.observableArrayList(new ArrayList<>(metrics.getFilesOrderedByModificationCount().entrySet())));
		ListView<File> lastCreatedList = new ListView<>(FXCollections.observableArrayList(metrics.getLastCreatedFilesOrDirectories(20)));
		ListView<File> lastAlteredList = new ListView<>(FXCollections.observableArrayList(metrics.getLastModifiedFiles(10)));
		VBox l1Box = new VBox(new Label("L1. Direct Descendants of a Directory"), descendantsList);
		VBox l2Box = new VBox(new Label("L2. Files Ordered by Modification Count"), filesAlteredList);
		VBox l3Box = new VBox(new Label("L3. Last 20 Created Files/Directories"), lastCreatedList);
		VBox l4Box = new VBox(new Label("L4. Last 10 Modified Files"), lastAlteredList);
		horizontalLayout.getChildren().addAll(l1Box, l2Box, l3Box, l4Box);
		ScrollPane scrollPane = new ScrollPane(horizontalLayout);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		dialog.getDialogPane().setContent(scrollPane);
		ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(closeButton);
		dialog.showAndWait();
	}
	/**
	 * Método das estatisticas
	 * @param event
	 */
	@FXML
	void handleStatistics(ActionEvent event) {
		Dialog<Void> dialog = new Dialog<>();
		dialog.initOwner(primaryStage);
		dialog.setTitle("Directory Statistics");
		Position<File> currentDirectoryPosition = pfs.treeLinked.root();
		File currentDirectory = currentDirectoryPosition.element();
		VBox verticalLayout = new VBox(10);
		long spaceOccupied = metrics.calculateSpaceOccupied(currentDirectory);
		int[] dirFileCount = metrics.countDirectoriesAndFiles(currentDirectory);
		int depth = metrics.calculateDepth(currentDirectory);
		List<File> topDirectories = metrics.findTop5DirectoriesWithMostDescendants(currentDirectory);
		Label spaceLabel = new Label("E1. Space Occupied: " + spaceOccupied + " bytes");
		Label countLabel = new Label("E2. Directories: " + dirFileCount[0] + ", Files: " + dirFileCount[1]);
		Label depthLabel = new Label("E3. Depth: " + depth);
		Label topDirsLabel = new Label("Top 5 Directories with Most Descendants: " + topDirectories.stream().map(File::getName).collect(Collectors.joining(", ")));
		verticalLayout.getChildren().addAll(spaceLabel, countLabel, depthLabel, topDirsLabel);
		ScrollPane scrollPane = new ScrollPane(verticalLayout);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		dialog.getDialogPane().setContent(scrollPane);
		ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(closeButton);
		dialog.showAndWait();
	}

	/**
	 * Método do contador de modificações
	 * @param event
	 */
	@FXML
	void handleShowModificationChart(ActionEvent event) {
		Map<String, Integer> modificationStats = metrics.getModificationStatsByMonth("2023");
		showChart("Number of Files/Directories Modified in 2023", modificationStats);
	}


	/**
	 * Método para o grafico de criação
	 * @param event
	 */
	@FXML
	private void handleShowCreationChart(ActionEvent event) {
		Map<String, Integer> creationStats = metrics.getCreationStatsByMonth("2023");
		showChart("Number of Files/Folders Created in 2023", creationStats);
	}

	/**
	 * Método do gráfico de barras
	 * @param title
	 * @param dataByMonth
	 */
	private void showChart(String title, Map<String, Integer> dataByMonth) {
	view.showChart(title,dataByMonth);
	}




	interface UndoableCommand {
		void undo();
	}

	/**
	 * CLASSE PADRAO COMMAND PARA UNDO DE RENAME
	 */
	class RenameCommand implements UndoableCommand {
		private File originalFile;
		private File renamedFile;

		public RenameCommand(File originalFile, File renamedFile) {
			this.originalFile = originalFile;
			this.renamedFile = renamedFile;
		}

		public void undo() {
			if (!renamedFile.renameTo(originalFile)) {
				throw new RuntimeException("Failed to undo rename from " + renamedFile + " to " + originalFile);
			}
		}

		public File getOriginalFile() {
			return originalFile;
		}

		public File getRenamedFile() {
			return renamedFile;
		}
	}

	/**
	 * CLASSE PADRAO COMMAND PARA UNDO DE CRIACAO DE FOLDERS
	 */
	class CreateFolderCommand implements UndoableCommand {
		private File folder;

		public CreateFolderCommand(File folder) {
			this.folder = folder;
		}

		public void undo() {
			if (folder.exists() && folder.isDirectory()) {
				File[] files = folder.listFiles();
				if (files != null && files.length > 0) {
					throw new RuntimeException("Folder is not empty: " + folder);
				}
				if (!folder.delete()) {
					throw new RuntimeException("Failed to delete folder: " + folder);
				}
			}
		}
	}


	/**
	 * CLASSE PADRAO COMMAND PARA UNDO DE CRIACAO DE FICHEIROS
	 */

	class CreateFileCommand implements UndoableCommand {
		private File file;

		public CreateFileCommand(File file) {
			this.file = file;
		}

		@Override
		public void undo() {
			if (file.exists()) {
				if (!file.delete()) {
					throw new RuntimeException("Failed to delete file: " + file);
				}
			}
		}
	}


	/**
	 * CLASSE PADRAO COMMAND PARA UNDO DE REMOÇÃO DE FICHEIROS/FOLDERS
	 */
	class DeleteFileCommand implements UndoableCommand {
		private File deletedFile;
		private File parentFolder;
		private boolean isDirectory;

		public DeleteFileCommand(File deletedFile, File parentFolder, boolean isDirectory) {
			this.deletedFile = deletedFile;
			this.parentFolder = parentFolder;
			this.isDirectory = isDirectory;
		}

		@Override
		public void undo() {
			if (isDirectory) {
				File restoredFolder = new File(parentFolder, deletedFile.getName());
				if (!restoredFolder.mkdir()) {
					throw new RuntimeException("Failed to restore directory: " + restoredFolder);
				}
			} else {
				File restoredFile = new File(parentFolder, deletedFile.getName());
				try {
					if (!restoredFile.createNewFile()) {
						throw new RuntimeException("Failed to restore file: " + restoredFile);
					}
				} catch (IOException e) {
					throw new RuntimeException("Failed to restore file: " + restoredFile, e);
				}
			}
		}
	}



	/**
	 * Command for undoing file edits.
	 */
	class EditFileCommand implements UndoableCommand {
		private File fileToEdit;
		private String originalContent;
		private String newContent;

		/**
		 * Constructs an EditFileCommand.
		 *
		 * @param fileToEdit       The file that was edited.
		 * @param originalContent  The original content of the file before the edit.
		 * @param newContent       The new content of the file after the edit.
		 */
		public EditFileCommand(File fileToEdit, String originalContent, String newContent) {
			this.fileToEdit = fileToEdit;
			this.originalContent = originalContent;
			this.newContent = newContent;
		}

		/**
		 * Undoes the edit operation by restoring the file's original content.
		 */
		@Override
		public void undo() {
			// Implement the logic to revert the file content back to originalContent
			try (FileWriter writer = new FileWriter(fileToEdit, false)) { // false to overwrite
				writer.write(originalContent);
			} catch (IOException e) {
				throw new RuntimeException("Failed to undo file edit: " + fileToEdit, e);
			}
		}
	}


	/**
	 * Undo para copy
	 */
	class CopyFileCommand implements UndoableCommand {
		private File sourceFile;
		private File destinationFile;

		public CopyFileCommand(File sourceFile, File destinationFile) {
			this.sourceFile = sourceFile;
			this.destinationFile = destinationFile;
		}

		@Override
		public void undo() {
			if (destinationFile.exists()) {
				deleteFileOrFolder(destinationFile);
			}
		}

		private void deleteFileOrFolder(File file) {
			if (file.isDirectory()) {
				for (File subFile : file.listFiles()) {
					deleteFileOrFolder(subFile);
				}
			}
			file.delete();
		}
	}


	/**
	 * UNDO PARA MOVE
	 */
	class MoveFileCommand implements UndoableCommand {
		private File originalFile;
		private File movedFile;
		private TreeItem<File> originalParentItem;
		private TreeItem<File> newParentItem;

		public MoveFileCommand(File originalFile, File movedFile, TreeItem<File> originalParentItem, TreeItem<File> newParentItem) {
			this.originalFile = originalFile;
			this.movedFile = movedFile;
			this.originalParentItem = originalParentItem;
			this.newParentItem = newParentItem;
		}

		@Override
		public void undo() {
			if (movedFile.renameTo(originalFile)) {
				newParentItem.getChildren().removeIf(item -> item.getValue().equals(movedFile));
				originalParentItem.getChildren().add(new TreeItem<>(originalFile));
			} else {
				throw new RuntimeException("Failed to undo file move from " + movedFile + " to " + originalFile);
			}
		}
	}



}
