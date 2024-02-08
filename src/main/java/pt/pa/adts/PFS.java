package pt.pa.adts;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;


/**
 * Classe que possui todas as operações atómicas e cálculo das métricas
 */
public class PFS {
    public TreeLinked<File> treeLinked = new TreeLinked<>();
    public Map<File, Boolean> lockStatusMap = new HashMap<>();
    public Map<File, String> fileContentsMap = new HashMap<>();

    public static final Logger logger = AppLogger.getLogger();

    public Map<File, Integer> creationCountMap = new HashMap<>();

    public Map<File, Integer> modificationCountMap = new HashMap<>();

    public Map<File, Date> modificationDateMap = new HashMap<>();

    public Map<String, Integer> monthlyCreationStats = new TreeMap<>();
    public Map<File, Integer> directoryDescendantCountMap = new HashMap<>();

    public Map<String, Integer> monthlyModificationStats = new TreeMap<>();
    /**
     * Método que insere elementos na TreeLinked
     * @param parent
     * @param file
     * @return
     */
    private Position<File> insertInTreeLinked(Position<File> parent, File file) {
        if (parent == null && treeLinked.isEmpty()) {
            treeLinked = new TreeLinked<>(file);
            return treeLinked.root();
        } else if (parent != null) {
            return treeLinked.insert(parent, file);
        } else {
            throw new IllegalArgumentException("Parent is null but TreeLinked is not empty.");
        }
    }


    /**
     * Método que cria pastas
     *
     * @param parent
     * @param folderName
     */
    public void createFolder(Position<File> parent, String folderName) {
        try {
            if (parent == null) {
                if (treeLinked.isEmpty()) {
                    treeLinked = new TreeLinked<>(new File(folderName));
                }
            } else {
                File parentFile = parent.element();
                File newFolder = new File(parentFile, folderName);

                if (!parentFile.isDirectory()) {
                    throw new IllegalArgumentException("Parent does not exist or is not a folder.");
                }
                if (!newFolder.mkdir()) {
                    throw new IllegalArgumentException("Cannot create folder.");
                }
                insertInTreeLinked(parent, newFolder);
                logger.info("Created a node in Treelinked" + " -> " + folderName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        File newFolder = new File(parent.element(), folderName);
        creationCountMap.put(newFolder, creationCountMap.getOrDefault(newFolder, 0) + 1);
        printTreeLinked();
    }


    /**
     * Cria um ficheiro
     * @param parent
     * @param fileName
     * @param isLocked
     * @param fileContent
     */
    public void createFile(Position<File> parent, String fileName, boolean isLocked, String fileContent) {
        File newFile = null;

        try {
            if (parent == null) {
                if (treeLinked.isEmpty()) {
                    newFile = new File(fileName);
                    if (!newFile.createNewFile()) {
                        throw new IllegalArgumentException("Cannot create root file.");
                    }
                    treeLinked = new TreeLinked<>(newFile);
                } else {
                    throw new IllegalArgumentException("Parent is null but TreeLinked is not empty.");
                }
            } else {
                File parentFile = parent.element();
                if (parentFile == null || !parentFile.exists() || !parentFile.isDirectory()) {
                    throw new IllegalArgumentException("Parent does not exist or is not a folder.");
                }

                newFile = new File(parentFile, fileName);

                boolean fileExistsOnDisk = newFile.exists();
                boolean fileExistsInTreeLinked = fileAlreadyExistsInTreeLinked(parent, fileName);

                if (!fileExistsOnDisk && !fileExistsInTreeLinked) {
                    if (!newFile.createNewFile()) {
                        throw new IllegalArgumentException("Cannot create file on disk.");
                    }
                    treeLinked.insert(parent, newFile);
                } else if (fileExistsOnDisk && !fileExistsInTreeLinked) {
                    treeLinked.insert(parent, newFile);
                } else {
                    throw new IllegalArgumentException("File already exists.");
                }
            }
            lockStatusMap.put(newFile, isLocked);
            fileContentsMap.put(newFile, fileContent);

            creationCountMap.put(newFile, creationCountMap.getOrDefault(newFile, 0) + 1);
            logger.info("Handled File Creation -> " + fileName + " in " + (parent != null ? parent.element().getAbsolutePath() : "Root"));
        } catch (Exception e) {
            System.err.println("Error handling file creation: " + e.getMessage());
            e.printStackTrace();
        }
        printTreeLinked();
    }

    /**
     * Verifica se já existe na TreeLinked
     * @param parent
     * @param fileName
     * @return
     */
    private boolean fileAlreadyExistsInTreeLinked(Position<File> parent, String fileName) {
        for (Position<File> child : treeLinked.children(parent)) {
            if (child.element().getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Verifica a posição
     * @param position
     * @throws InvalidPositionException
     */
    private void checkPosition(Position<File> position) throws InvalidPositionException {
        if (position == null) {
            throw new InvalidPositionException("Position is null.");
        }
    }

    /**
     * Método para verificar se ficheiro esta locked
     * @param position
     */
    public boolean isLocked(Position<File> position) {
        checkPosition(position);
        File file = position.element();
        return lockStatusMap.getOrDefault(file, true);
    }

    /**
     * Método para bloquear ficheiro
     * @param position
     */
    public void lockFile(Position<File> position) {
        checkPosition(position);
        File file = position.element();
        lockStatusMap.put(file, true);
        logger.info("File locked");
    }

    /**
     * Método para desbloquear ficheiro
     * @param position
     */
    public void unlockFile(Position<File> position) {
        checkPosition(position);
        File file = position.element();
        lockStatusMap.put(file, false);
        logger.info("File unlcoked");
    }

    /**
     * Método para editar ficheiro
     * @param position
     * @param newContent
     */
    public void editFile(Position<File> position, String newContent) {
        if (position != null) {
            File file = position.element();
            if (!isLocked(position)) {
                fileContentsMap.put(file, newContent);
                logger.info("File Edited, NEW INFO:" + " -----> " + newContent);
                modificationCountMap.put(file, modificationCountMap.getOrDefault(file, 0) + 1);
                modificationDateMap.put(file, new Date());
            } else {
                logger.info("Cannot edit because file is locked");
            }
        } else {
            logger.severe("Position is null. Cannot edit the file.");
        }
    }


    /**
     * Método para eliminar um elemento
     * @param elementPosition
     */
    public void deleteElement(Position<File> elementPosition) {
        try {
            if (elementPosition == null) {
                throw new IllegalArgumentException("Element position cannot be null.");
            }

            File elementFile = elementPosition.element();
            Position<File> parentPosition = treeLinked.parent(elementPosition);
            if (treeLinked.isInternal(elementPosition)) {
                Iterator<Position<File>> childrenIterator = treeLinked.children(elementPosition).iterator();
                if (childrenIterator.hasNext()) {
                    return;
                }

            }
            treeLinked.remove(elementPosition);
            lockStatusMap.remove(elementFile);
            fileContentsMap.remove(elementFile);

            if (parentPosition != null) {
                Position<File> elementToRemove = findPositionWithName(parentPosition, elementFile.getName());

                if (elementToRemove != null) {
                    treeLinked.remove(elementToRemove);

                }
            }
            logger.info("Node Deleted--->" + elementFile.getName());

        } catch (InvalidPositionException e) {
            e.printStackTrace();
        }
        printTreeLinked();
    }

    /**
     * Método para dar reaneme
     * @param elementPosition
     * @param newName
     */
    public void renameElement(Position<File> elementPosition, String newName) {
        try {
            checkPosition(elementPosition);

            File currentFile = elementPosition.element();

            Position<File> existingPosition = findPositionWithName(elementPosition, newName);
            if (existingPosition != null) {
                throw new IllegalArgumentException("An element with the provided name already exists.");
            }

            treeLinked.replace(elementPosition, new File(currentFile.getParent(), newName));
            lockStatusMap.put(new File(currentFile.getParent(), newName), lockStatusMap.remove(currentFile));
            fileContentsMap.put(new File(currentFile.getParent(), newName), fileContentsMap.remove(currentFile));

            logger.info("Node Renamed--->" + newName);
            File file = elementPosition.element();
            modificationCountMap.put(file, modificationCountMap.getOrDefault(file, 0) + 1);
            modificationDateMap.put(file, new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Método para encontrar posição pelo nome
     * @param startPosition
     * @param name
     * @return
     */
    private Position<File> findPositionWithName(Position<File> startPosition, String name) {
        for (Position<File> position : treeLinked.children(startPosition)) {
            if (position.element().getName().equals(name)) {
                return position;
            }
        }
        return null;
    }

    /**
     * Método para copiar um elemento
     * @param source
     * @param destination
     */
    public void copyElement(Position<File> source, Position<File> destination) {
        try {
            if (source == null || destination == null) {
                throw new IllegalArgumentException("Source and destination positions cannot be null.");
            }

            File sourceFile = source.element();
            File destinationFile = destination.element();

            if (sourceFile.equals(destinationFile) || isDescendant(source, destination)) {
                throw new IllegalArgumentException("Invalid copy operation.");
            }
            copySubtree(source, destination);

            logger.info("Node Copied to ---> " + destinationFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método para verificar se é descendete
     * @param source
     * @param destination
     * @return
     */
    private boolean isDescendant(Position<File> source, Position<File> destination) {
        if (treeLinked.isRoot(source)) return false; // Root can't be a descendant of anything

        Position<File> parent = treeLinked.parent(source);
        while (parent != null) {
            if (parent.equals(destination)) return true;
            parent = treeLinked.parent(parent);
        }
        return false;
    }

    /**
     * Método que copia uma sub árvore
     * @param source
     * @param destinationParent
     */
    private void copySubtree(Position<File> source, Position<File> destinationParent) {
        File sourceFile = source.element();
        File newFile = new File(destinationParent.element(), sourceFile.getName());
        Position<File> newDestination = treeLinked.insert(destinationParent, newFile);
        if (fileContentsMap.containsKey(sourceFile)) {
            String content = fileContentsMap.get(sourceFile);
            fileContentsMap.put(newFile, content);
        }
        for (Position<File> child : treeLinked.children(source)) {
            copySubtree(child, newDestination);
        }
    }


    /**
     * Método para mover um elemento
     * @param sourcePosition
     * @param destinationPosition
     */
    public void moveElement(Position<File> sourcePosition, Position<File> destinationPosition) {
        try {
            if (sourcePosition == null || destinationPosition == null) {
                throw new IllegalArgumentException("Source and destination positions cannot be null.");
            }

            File element = sourcePosition.element();
            Position<File> sourceParent = treeLinked.parent(sourcePosition);

            if (sourceParent == null) {
                return;
            }
            treeLinked.remove(sourcePosition);

            treeLinked.insert(destinationPosition, element);

            logger.info("Element Moved ---> " + element.getName() + "to " + destinationPosition);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Método para encontrar posição na Treelinked para mover
     * @param treeLinked
     * @param sourceFile
     * @param destinationFolder
     * @return
     */
    public Position<File> findPositionInTreeLinkedForMove(TreeLinked<File> treeLinked, File sourceFile, File destinationFolder) {
        return findPositionInTreeLinkedForMove(treeLinked.root(), sourceFile, destinationFolder);
    }

    /**
     * Método para encontrar posição na treelinked para mover
     * @param currentPosition
     * @param sourceFile
     * @param destinationFolder
     * @return
     */
    public Position<File> findPositionInTreeLinkedForMove(Position<File> currentPosition, File sourceFile, File destinationFolder) {
        if (currentPosition == null) {
            return null;
        }

        File currentElement = currentPosition.element();

        if (currentElement.equals(sourceFile)) {
            return currentPosition;
        }

        if (currentElement.equals(destinationFolder)) {
            return currentPosition;
        }

        for (Position<File> childPosition : treeLinked.children(currentPosition)) {
            Position<File> foundPosition = findPositionInTreeLinkedForMove(childPosition, sourceFile, destinationFolder);
            if (foundPosition != null) {
                return foundPosition;
            }
        }

        return null;
    }

    /**
     * Método para adicionar conteúdos à TreeLinked
     * @param directory
     * @param treeLinked
     * @param parentPosition
     */
    public void addContentsToTreeLinked(File directory, TreeLinked<File> treeLinked, Position<File> parentPosition) {
        if (directory == null || !directory.exists()) {
            return;
        }
        Position<File> currentPosition = insertInTreeLinked(directory, treeLinked, parentPosition);
        addDirectoryContents(directory, treeLinked, currentPosition);
    }

    private void addDirectoryContents(File directory, TreeLinked<File> treeLinked, Position<File> parentPosition) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    addContentsToTreeLinked(file, treeLinked, parentPosition);
                }
            }
        }
    }

    private Position<File> insertInTreeLinked(File file, TreeLinked<File> treeLinked, Position<File> parentPosition) {
        if (parentPosition == null) {
            return treeLinked.insert(null, file);
        } else {
            return treeLinked.insert(parentPosition, file);
        }
    }


    public Position<File> findPositionInTreeLinked(File parentFolder, String name) {
        if (parentFolder == null || name == null) {
            return null;
        }

        for (Position<File> position : treeLinked.positions()) {
            File element = position.element();
            if (element.isDirectory() && element.getAbsolutePath().equals(parentFolder.getAbsolutePath())) {
                for (Position<File> childPosition : treeLinked.children(position)) {
                    if (childPosition.element().getName().equals(name)) {
                        return childPosition;
                    }
                }
                File newChildFolder = new File(parentFolder, name);
                Position<File> newChildPosition = treeLinked.insert(position, newChildFolder);
                return newChildPosition;
            }
        }

        return null;
    }


    /**
     * Método que encontra a posição de um folder na TreeLinked
     * @param folder
     * @return
     */
    public Position<File> findFolderPositionInTreeLinked(File folder) {
        if (folder == null) {
            return null;
        }

        for (Position<File> position : treeLinked.positions()) {
            File element = position.element();
            if (element.isDirectory() && element.getAbsolutePath().equals(folder.getAbsolutePath())) {
                return position;
            }
        }

        return null;
    }




    /**
     * Devolve a raiz da treelinked
     * @return
     */
    public File getRootOfTreeLinked() {
        return treeLinked.isEmpty() ? null : treeLinked.root().element();
    }


    /**
     * Faz uma copia do PFS
     * @return
     */
    @Override
    public PFS clone() {
        try {
            return (PFS) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Imprime a TreeLinked
     */
    public void printTreeLinked() {
        if (treeLinked != null && !treeLinked.isEmpty()) {
            System.out.println("Printing TreeLinked Structure:");
            printTreeNode(treeLinked.root(), 0);
        } else {
            System.out.println("TreeLinked is empty or not initialized.");
        }
    }

    /**
     * Método para imprimir teenode
     * @param node
     * @param level
     */
    private void printTreeNode(Position<File> node, int level) {
        if (node == null) {
            return;
        }
        File file = node.element();
        String indent = " ".repeat(level * 2);
        System.out.println(indent + "- " + (file != null ? file.getAbsolutePath() : "null"));

        for (Position<File> child : treeLinked.children(node)) {
            printTreeNode(child, level + 1);
        }
    }

    /**
     * Encontra o pai de um ficheiro
     * @param parentFolder
     * @return
     */
    public Position<File> findParentPositionForFile(File parentFolder) {
        if (parentFolder == null) {
            return null;
        }

        for (Position<File> position : treeLinked.positions()) {
            File element = position.element();
            if (element.isDirectory() && element.getAbsolutePath().equals(parentFolder.getAbsolutePath())) {
                return position;
            }
        }

        return null;
    }
}
