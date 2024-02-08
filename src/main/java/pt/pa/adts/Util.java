package pt.pa.adts;

import java.io.File;
import java.util.Comparator;

import javafx.scene.control.TreeItem;
public class Util {

    /** Um tree item compator para organizar os nodes numa arvore. */
    public static final Comparator<? super TreeItem<File>> COMPARATOR = new Comparator<>() {
        @Override
        public int compare(TreeItem<File> o1, TreeItem<File> o2) {
            return o1.getValue().toPath().compareTo(o2.getValue().toPath());
        }
    };
}
