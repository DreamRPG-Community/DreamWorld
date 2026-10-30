package cn.mythicland.dreamworldprotect.storage;

import cn.mythicland.dreamlib.path.SafePathResolver;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves one safe configuration file for each Bukkit world.
 */
public final class WorldConfigFileResolver {

    private final Path root;
    private final SafePathResolver resolver;

    public WorldConfigFileResolver(Path root) {
        this.root = root.toAbsolutePath().normalize();
        this.resolver = new SafePathResolver(this.root);
    }

    public void ensureRootDirectory() throws IOException {
        resolver.ensureRootDirectory();
    }

    public Path resolve(World world, String logicalName) throws IOException {
        String name = logicalName == null || logicalName.isBlank() ? world.getName() : logicalName;
        try {
            String safeName = resolver.normalizeSingleSegment(name);
            return resolver.resolveSingleSegment(safeName + ".yml");
        } catch (IllegalArgumentException exception) {
            Path uuidDirectory = root.resolve("by-uuid").normalize();
            if (!uuidDirectory.startsWith(root) || uuidDirectory.equals(root)) {
                throw new IOException("World configuration fallback path is invalid", exception);
            }
            if (Files.isSymbolicLink(uuidDirectory)) {
                throw new IOException("World configuration fallback directory is a symbolic link: "
                        + uuidDirectory);
            }
            Files.createDirectories(uuidDirectory);
            return uuidDirectory.resolve(world.getUID() + ".yml").normalize();
        }
    }
}
