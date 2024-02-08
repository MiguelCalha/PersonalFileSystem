package pt.pa.adts;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;

/**
 * Classe para separar a lógica de adicionar  filhos à árvore
 */
public class LazyTreeItem extends TreeItem<File> {
    private boolean isFirstTimeChildren = true;

    public LazyTreeItem(File file) {
        super(file);
        this.setValue(file);
    }

    /**
     * Metodo  para obter filhos
     * @return
     */
    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            if (getValue().isDirectory()) {
                File[] files = getValue().listFiles();
                if (files != null) {
                    for (File childFile : files) {
                        super.getChildren().add(new LazyTreeItem(childFile));
                    }
                }
            }
        }
        return super.getChildren();
    }

    /**
     * Metodo para verificar se é folha
     * @return
     */
    @Override
    public boolean isLeaf() {
        return getValue().isFile();
    }
}