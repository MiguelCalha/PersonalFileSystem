package pt.pa.adts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Classe que calcula as Métricas da aplicação
 */
public class Metrics {

    private PFS pfs;

    public Metrics() {
        this.pfs = new PFS();
    }

    /**
     * L1. List Direct Descendants of a Directory
     * @param directory
     * @param type
     * @return
     */
    public List<File> getDirectDescendants(Position<File> directory, FileType type) {
        List<File> descendants = new ArrayList<>();
        if (directory.element().isDirectory()) {
            for (Position<File> child : pfs.treeLinked.children(directory)) {
                File file = child.element();
                if ((type == FileType.FILE && file.isFile()) ||
                        (type == FileType.DIRECTORY && file.isDirectory()) ||
                        (type == FileType.BOTH)) {
                    descendants.add(file);
                }
            }
        }
        return descendants;
    }

    /**
     * L2. Total Number of Changed Files, Ordered by Total Modifications
     * @return
     */
    public LinkedHashMap<File, Integer> getFilesOrderedByModificationCount() {
        return pfs.modificationCountMap.entrySet()
                .stream()
                .sorted(Map.Entry.<File, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }


    /**
     * L3. Last 20 Created Files/Directories
     * @param limit
     * @return
     */
    public List<File> getLastCreatedFilesOrDirectories(int limit) {
        return StreamSupport.stream(pfs.treeLinked.positions().spliterator(), false)
                .map(Position::element)
                .sorted(Comparator.comparing(this::getCreationDate).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Retorna a data der criação
     * @param file
     * @return
     */
    public Date getCreationDate(File file) {
        Path filePath = file.toPath();
        BasicFileAttributes attributes = null;

        try {
            attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        } catch (IOException e) {
            pfs.logger.severe("Unable to read creation date for file: " + file.getAbsolutePath());
            e.printStackTrace();
            return null;
        }

        FileTime creationTime = attributes.creationTime();
        return new Date(creationTime.toMillis());
    }

    /**
     * L4. Last 10 Modified Files
     * @param limit
     * @return
     */
    public List<File> getLastModifiedFiles(int limit) {
        return pfs.modificationDateMap.entrySet().stream()
                .sorted(Map.Entry.<File, Date>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get the number of modifications for a given file.
     *
     * @param file The file for which to get the modification count.
     * @return The number of times the file has been modified.
     */
    public int getModificationCount(File file) {
        if (pfs.modificationCountMap.containsKey(file)) {
            return pfs.modificationCountMap.get(file);
        } else {
            return 0;
        }
    }

    /**
     * E1. Espaço Ocupado pelos Ficheiros
     *
     * @param directory
     * @return
     */
    public long calculateSpaceOccupied(File directory) {
        long totalSpace = 0;
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    totalSpace += file.length();
                } else if (file.isDirectory()) {
                    totalSpace += calculateSpaceOccupied(file); // Recursive call for subdirectories
                }
            }
        }
        return totalSpace;
    }

    /**
     * E2. Nº de Diretorias e Nº de Ficheiros Totais
     *
     * @param directory
     * @return
     */
    public int[] countDirectoriesAndFiles(File directory) {
        int[] count = {0, 0};
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    count[1]++;
                } else if (file.isDirectory()) {
                    count[0]++;
                    int[] subCount = countDirectoriesAndFiles(file);
                    count[0] += subCount[0];
                    count[1] += subCount[1];
                }
            }
        }
        return count;
    }

    /**
     * E3. Profundidade
     * @param directory
     * @return
     */
    public int calculateDepth(File directory) {
        int depth = 0;
        File parent = directory.getParentFile();
        while (parent != null) {
            depth++;
            parent = parent.getParentFile();
        }
        return depth;
    }

    /**
     * Top 5 Diretorias com Maior Número de Descendentes
     *
     * @param directory
     * @return
     */
    public List<File> findTop5DirectoriesWithMostDescendants(File directory) {
        traverseAndCountDescendants(directory, pfs.directoryDescendantCountMap);
        List<File> sortedDirectories = pfs.directoryDescendantCountMap.keySet()
                .stream()
                .sorted((dir1, dir2) -> Integer.compare(pfs.directoryDescendantCountMap.get(dir2), pfs.directoryDescendantCountMap.get(dir1)))
                .limit(5)
                .collect(Collectors.toList());

        return sortedDirectories;
    }

    /**
     * Contar os descendentes de uma pasta recursivamente
     * @param directory
     * @param countMap
     * @return
     */
    private int traverseAndCountDescendantsRecursive(File directory, Map<File, Integer> countMap) {
        int descendantCount = 0;
        if (directory.isDirectory()) {
            descendantCount = directory.listFiles().length; // Initialize with direct descendants
            for (File subDirectory : directory.listFiles()) {
                if (subDirectory.isDirectory()) {
                    descendantCount += traverseAndCountDescendantsRecursive(subDirectory, countMap);
                }
            }
            countMap.put(directory, descendantCount);
        }
        return descendantCount; // Return the count
    }

    /**
     * Contar os descendentes de uma pasta
     * @param directory
     * @param countMap
     * @return
     */
    private int traverseAndCountDescendants(File directory, Map<File, Integer> countMap) {
        int descendantCount = 0;
        if (directory.isDirectory()) {
            descendantCount = traverseAndCountDescendantsRecursive(directory, countMap);
        }
        return descendantCount;
    }


    /**
     * Método que reune as estatisticas de criação por  mes
     *
     * @param year
     * @return
     */
    public Map<String, Integer> getCreationStatsByMonth(String year) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");

        for (Position<File> position : pfs.treeLinked.positions()) {
            File file = position.element();
            Date creationDate = getCreationDate(file);
            String creationMonth = monthFormat.format(creationDate);

            if (creationMonth.startsWith(year)) {
                pfs.monthlyCreationStats.put(creationMonth, pfs.monthlyCreationStats.getOrDefault(creationMonth, 0) + 1);
            }
        }

        return pfs.monthlyCreationStats;
    }


    /**
     * Retorna as stats de modificação do mês
     * @param year
     * @return
     */
    public Map<String, Integer> getModificationStatsByMonth(String year) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");

        for (Map.Entry<File, Date> entry : pfs.modificationDateMap.entrySet()) {
            File file = entry.getKey();
            Date modificationDate = entry.getValue();
            String modificationMonth = monthFormat.format(modificationDate);

            if (modificationMonth.startsWith(year)) {
                pfs.monthlyModificationStats.put(modificationMonth, pfs.monthlyModificationStats.getOrDefault(modificationMonth, 0) + 1);
            }
        }

        return pfs.monthlyModificationStats;
    }
}
