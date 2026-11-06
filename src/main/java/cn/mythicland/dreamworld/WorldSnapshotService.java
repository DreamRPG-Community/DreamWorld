package cn.mythicland.dreamworld;

import cn.mythicland.framework.path.FileTreeOperations;
import cn.mythicland.framework.path.SafePathResolver;
import cn.mythicland.framework.bukkit.world.v1_12_R1.LegacyRegionSnapshots;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Stream;

final class WorldSnapshotService {

    private static final Set<String> SNAPSHOT_DIMENSIONS = Set.of("", "DIM-1", "DIM1");

    private final SafePathResolver snapshotResolver;
    private final SafePathResolver workResolver;
    private final RegionCopier regionCopier;

    WorldSnapshotService(Path snapshotsRoot, Path workRoot) {
        this(snapshotsRoot, workRoot, LegacyRegionSnapshots::copyWithoutCreatures);
    }

    WorldSnapshotService(Path snapshotsRoot, Path workRoot, RegionCopier regionCopier) {
        this.snapshotResolver = new SafePathResolver(snapshotsRoot);
        this.workResolver = new SafePathResolver(workRoot);
        this.regionCopier = regionCopier;
    }

    void ensureRootDirectory() throws IOException {
        snapshotResolver.ensureRootDirectory();
        workResolver.ensureRootDirectory();
    }

    SnapshotSaveResult save(String logicalName, Path worldDirectory) throws IOException {
        WorldDirectoryValidator.requireWorldDirectory(worldDirectory);

        Path snapshotDirectory = snapshotResolver.resolveSingleSegment(logicalName);
        Path temporaryDirectory = workResolver.root().resolve(".snapshot-" + UUID.randomUUID());
        Files.createDirectory(temporaryDirectory);
        try {
            int copiedEntries = copyMapFiles(worldDirectory, temporaryDirectory);
            FileTreeOperations.removeEmptyDirectories(temporaryDirectory);
            replaceSnapshot(temporaryDirectory, snapshotDirectory);
            return new SnapshotSaveResult(copiedEntries);
        } finally {
            deleteRecursivelyIfPresent(temporaryDirectory);
        }
    }

    SnapshotRestoreResult restore(String logicalName, Path worldDirectory) throws IOException {
        PreparedRestore prepared = prepareRestore(logicalName, worldDirectory);
        copyRegions(prepared.mapCopy.regions());
        return prepared.result();
    }

    PreparedRestore prepareRestore(String logicalName, Path worldDirectory) throws IOException {
        Path snapshotDirectory = snapshotResolver.resolveSingleSegment(logicalName);
        WorldDirectoryValidator.requireWorldDirectory(snapshotDirectory);
        prepareTargetDirectory(worldDirectory);

        int deletedEntries = 0;
        try (Stream<Path> children = Files.list(worldDirectory)) {
            for (Path child : children.toList()) {
                deletedEntries += FileTreeOperations.deleteRecursively(child);
            }
        }

        return new PreparedRestore(deletedEntries, prepareMapCopy(snapshotDirectory, worldDirectory));
    }

    private int copyMapFiles(Path sourceDirectory, Path targetDirectory) throws IOException {
        MapCopy prepared = prepareMapCopy(sourceDirectory, targetDirectory);
        copyRegions(prepared.regions());
        return prepared.metadataEntries() + prepared.regions().size();
    }

    private void copyRegions(List<RegionCopy> regions) throws IOException {
        for (RegionCopy region : regions) regionCopier.copy(region.source(), region.target());
    }

    private MapCopy prepareMapCopy(Path sourceDirectory, Path targetDirectory) throws IOException {
        int copiedEntries = 0;
        copyRequiredFile(sourceDirectory.resolve("level.dat"), targetDirectory.resolve("level.dat"));
        copiedEntries++;

        Path sourceUid = sourceDirectory.resolve("uid.dat");
        if (Files.exists(sourceUid, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(sourceUid)) {
            copyRequiredFile(sourceUid, targetDirectory.resolve("uid.dat"));
            copiedEntries++;
        }

        List<RegionCopy> regions = new ArrayList<>();
        for (String dimension : SNAPSHOT_DIMENSIONS) {
            Path sourceDimension = dimension.isEmpty() ? sourceDirectory : sourceDirectory.resolve(dimension);
            Path targetDimension = dimension.isEmpty() ? targetDirectory : targetDirectory.resolve(dimension);
            requireDirectoryOrAbsent(sourceDimension);
            requireDirectoryOrAbsent(targetDimension);
            Path sourceRegion = sourceDimension.resolve("region");
            Path targetRegion = targetDimension.resolve("region");
            regions.addAll(findRegionFiles(sourceRegion, targetRegion));
        }
        return new MapCopy(copiedEntries, List.copyOf(regions));
    }

    private void copyRequiredFile(Path source, Path target) throws IOException {
        if (Files.isSymbolicLink(source)) {
            throw new IOException("World resource cannot be a symbolic link: " + source);
        }
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("World resource is not a regular file: " + source);
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private List<RegionCopy> findRegionFiles(Path sourceRegion, Path targetRegion) throws IOException {
        if (!Files.exists(sourceRegion, LinkOption.NOFOLLOW_LINKS)) return List.of();
        if (Files.isSymbolicLink(sourceRegion)) {
            throw new IOException("World region directory cannot be a symbolic link: " + sourceRegion);
        }
        if (!Files.isDirectory(sourceRegion, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("World region path is not a directory: " + sourceRegion);
        }

        List<RegionCopy> regions = new ArrayList<>();
        Files.walkFileTree(sourceRegion, new SimpleFileVisitor<>() {
            @Override
            public @Nonnull FileVisitResult preVisitDirectory(
                    @Nonnull Path directory,
                    @Nonnull BasicFileAttributes attributes
            )
                    throws IOException {
                if (Files.isSymbolicLink(directory)) {
                    throw new IOException("World region directory cannot be a symbolic link: " + directory);
                }
                Path targetDirectory = targetRegion.resolve(sourceRegion.relativize(directory));
                Files.createDirectories(targetDirectory);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @Nonnull FileVisitResult visitFile(
                    @Nonnull Path file,
                    @Nonnull BasicFileAttributes attributes
            ) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    throw new IOException("World region file cannot be a symbolic link: " + file);
                }
                if (!attributes.isRegularFile() || !file.getFileName().toString().endsWith(".mca")) {
                    return FileVisitResult.CONTINUE;
                }

                Path target = targetRegion.resolve(sourceRegion.relativize(file)).normalize();
                if (!target.startsWith(targetRegion)) {
                    throw new IOException("World region file escapes the snapshot directory: " + file);
                }
                Files.createDirectories(target.getParent());
                regions.add(new RegionCopy(file, target));
                return FileVisitResult.CONTINUE;
            }
        });
        return regions;
    }

    /**
     * Executes one prepared restore; each region is independent and the source remains read-only.
     */
    final class PreparedRestore {
        private final int deletedEntries;
        private final MapCopy mapCopy;

        private PreparedRestore(int deletedEntries, MapCopy mapCopy) {
            this.deletedEntries = deletedEntries;
            this.mapCopy = mapCopy;
        }

        CompletableFuture<SnapshotRestoreResult> copyAsync(Function<Runnable, CompletableFuture<Void>> runAsync) {
            List<CompletableFuture<Void>> pending = new ArrayList<>();
            for (RegionCopy region : mapCopy.regions()) {
                pending.add(runAsync.apply(() -> {
                    try {
                        regionCopier.copy(region.source(), region.target());
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                }));
            }
            return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> result());
        }

        private SnapshotRestoreResult result() {
            return new SnapshotRestoreResult(deletedEntries, mapCopy.metadataEntries() + mapCopy.regions().size());
        }
    }

    private record MapCopy(int metadataEntries, List<RegionCopy> regions) {
    }

    private record RegionCopy(Path source, Path target) {
    }

    @FunctionalInterface
    interface RegionCopier {
        void copy(Path source, Path target) throws IOException;
    }

    private void requireDirectoryOrAbsent(Path directory) throws IOException {
        if (Files.isSymbolicLink(directory)) {
            throw new IOException("World dimension cannot be a symbolic link: " + directory);
        }
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("World dimension is not a directory: " + directory);
        }
    }

    private void replaceSnapshot(Path temporaryDirectory, Path snapshotDirectory) throws IOException {
        Path previousDirectory = workResolver.root().resolve(".previous-" + UUID.randomUUID());
        boolean movedPrevious = false;
        try {
            if (Files.exists(snapshotDirectory, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(snapshotDirectory)) {
                Files.move(snapshotDirectory, previousDirectory);
                movedPrevious = true;
            }
            Files.move(temporaryDirectory, snapshotDirectory);
            if (movedPrevious) deleteRecursivelyIfPresent(previousDirectory);
        } catch (IOException exception) {
            if (movedPrevious && !Files.exists(snapshotDirectory, LinkOption.NOFOLLOW_LINKS)) {
                Files.move(previousDirectory, snapshotDirectory);
            }
            throw exception;
        } finally {
            deleteRecursivelyIfPresent(previousDirectory);
        }
    }

    private void prepareTargetDirectory(Path worldDirectory) throws IOException {
        if (Files.isSymbolicLink(worldDirectory)) {
            throw new IOException("World directory cannot be a symbolic link: " + worldDirectory);
        }
        if (Files.exists(worldDirectory, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(worldDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("World path is not a directory: " + worldDirectory);
        }
        Files.createDirectories(worldDirectory);
    }

    private void deleteRecursivelyIfPresent(Path path) throws IOException {
        FileTreeOperations.deleteRecursively(path);
    }

    record SnapshotSaveResult(int copiedEntries) {
    }

    record SnapshotRestoreResult(int deletedEntries, int copiedEntries) {
    }

}
