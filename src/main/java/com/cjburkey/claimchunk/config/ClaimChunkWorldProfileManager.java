package com.cjburkey.claimchunk.config;

import com.cjburkey.claimchunk.ClaimChunk;
import com.cjburkey.claimchunk.Utils;
import com.cjburkey.claimchunk.config.access.Access;
import com.cjburkey.claimchunk.config.access.BlockAccess;
import com.cjburkey.claimchunk.config.access.EntityAccess;
import com.cjburkey.claimchunk.config.ccconfig.*;
import org.bukkit.Material;
import org.bukkit.entity.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

// TODO: ADD TONS OF COMMENTS THIS IS SO RIDICULOUS!
public class ClaimChunkWorldProfileManager {

    private static final String HEADER_COMMENT
            = "This config was last loaded with ClaimChunk version %s\n\n"
            + "This is the per-world config file for the world \"%s\"\n\n"
            + "   _    _      _\n"
            + "  | |  | |    | |\n"
            + "  | |__| | ___| |_ __\n"
            + "  |  __  |/ _ \\ | '_ \\\n"
            + "  | |  | |  __/ | |_) |\n"
            + "  |_|  |_|\\___|_| .__/\n"
            + "                | |\n"
            + "                |_|\n"
            + " -----------------------\n\n"
            + "Each label has `claimedChunks` or `unclaimedChunks` and `blockAccesses` or `entitiesAccesses`\n"
            + "Under each label, the code name of either an entity or block appears, followed by the protections (order for protections does *NOT* matter).\n"
            + "Protections with a value of `true` will be allowed, those with a value of `false` will not."
            + "For blocks, the protections are: `B` for breaking, `P` for placing, `I` for interacting, and `E` for exploding.\n"
            + "For entities, the protections are: `D` for damaging, `I` for interacting, and `E` for exploding.\n"    // hehe, "DIE" lol
            + "Note: These protections (except for exploding) are purely player-based.\n"
            + "I.e. `D` for damaging entities, when set to `D:false` will prevent players from damaging the entity.\n\n"
            + "Examples:\n\n"
            + "To allow only interacting with all blocks in unclaimed chunks in this world:\n\n"
            + "unclaimedChunks.blockAccesses:\n"
            + "  " + ClaimChunkWorldProfile.DEFAULT + ":  I:true B:false P:false E:false ;\n\n"
            + "(Note: the key `" + ClaimChunkWorldProfile.DEFAULT + "` can be used to mean \"all blocks/entities will have this if they are not defined here\")\n\n"
            + "Finally, the `_` label is for world properties. These will not vary between unclaimed and claimed chunks.\n\n"
            // TODO: MAKE WIKI PAGE
            + "More information is available on the GitHub wiki: https://github.com/cjburkey01/ClaimChunk/wiki\n";

    // Default profile is initialized when it is needed first
    private ClaimChunkWorldProfile defaultProfile = null;

    private final ClaimChunk claimChunk;

    // Config management
    private final File worldConfigDir;
    private final HashMap<String, ClaimChunkWorldProfile> profiles;

    private final CCConfigParser parser;
    private final CCConfigWriter writer;

    public ClaimChunkWorldProfileManager(@Nonnull ClaimChunk claimChunk,
                                         @Nonnull File worldConfigDir,
                                         @Nonnull CCConfigParser parser,
                                         @Nonnull CCConfigWriter writer) {
        this.claimChunk = claimChunk;

        this.worldConfigDir = worldConfigDir;
        profiles = new HashMap<>();

        this.parser = parser;
        this.writer = writer;
    }

    public @Nonnull ClaimChunkWorldProfile getProfile(@Nonnull String worldName) {
        // Try to get the config from the ones already loaded
        return profiles.computeIfAbsent(worldName, n -> {
            File file = new File(worldConfigDir, n + ".txt");

            // Create a config handle for this file
            CCConfigHandler<CCConfig> cfg = new CCConfigHandler<>(
                    file,
                    new CCConfig(String.format(HEADER_COMMENT, claimChunk.getVersion(), worldName), "")
            );

            // Set the base config to the default template so new options will
            // be forced into the config
            getDefaultProfile().toCCConfig(cfg.config());

            // Whether the file can be rewritten without losing data
            boolean canReformat = true;

            // Make sure the file exists duh
            if (file.exists()) {
                // Try to parse the config
                if (cfg.load((input, ncgf) -> {
                    List<CCConfigParseError> errors = parser.parse(ncgf, input);
                    for (CCConfigParseError error : errors) {
                        Utils.err("Error parsing file \"%s\"", file.getPath());
                        Utils.err("Description: %s", error);
                    }
                })) {
                    Utils.debug("Loaded world config file \"%s\"", file.getPath());
                } else {
                    canReformat = false;
                    Utils.err("Failed to load world config file \"%s\"", file.getPath());
                }
            }

            // Create the new world profile and override defaults with the
            // loaded values
            ClaimChunkWorldProfile profile = new ClaimChunkWorldProfile(false,
                                                                        null,
                                                                        null);
            profile.fromCCConfig(cfg.config());

            if (canReformat) {
                // Save the config to make sure that any new options will be loaded in
                boolean existed = file.exists();
                if (cfg.save(writer::serialize)) {
                    Utils.debug("%s world config file \"%s\"", (existed ? "Updated" : "Created"), file.getPath());
                } else {
                    Utils.err("Failed to save world config file at \"%s\"", file.getPath());
                }
            }
            return profile;
        });
    }

    /*
        Normally, I'd add a save method, but these world profiles should be
        immutable. There are *certain* things that can be modified, such as
        the entities or blocks lists, but those changes shouldn't be saved as
        there shouldn't be any persistent changes made at runtime to prevent
        unexpected behavior when, for example, uninstalling an addon that
        modified the list and having to manually remove or re-add entries to the
        profile file.
    */

    public void reloadAllProfiles() {
        // Clearing all the worlds will require them to be loaded again
        profiles.clear();
    }

    // API method
    @SuppressWarnings("unused")
    public void setDefaultProfile(ClaimChunkWorldProfile profile) {
        defaultProfile = profile;
    }

    public @Nonnull ClaimChunkWorldProfile getDefaultProfile() {
        // Lazy initialization; if the default profile hasn't been built yet,
        // build one
        if (defaultProfile == null) {
            // Initialize the profile access components
            final Access claimedChunks = new Access(new HashMap<>(), new HashMap<>());
            final Access unclaimedChunks = new Access(new HashMap<>(), new HashMap<>());

            // Assign entity defaults
            claimedChunks.entityAccesses.put(EntityType.UNKNOWN,
                    new EntityAccess(false, false, false));
            unclaimedChunks.entityAccesses.put(EntityType.UNKNOWN,
                    new EntityAccess(true, true, true));

            // Assign block defaults
            claimedChunks.blockAccesses.put(Material.AIR,
                    new BlockAccess(false, false, false, false));
            unclaimedChunks.blockAccesses.put(Material.AIR,
                    new BlockAccess(true, true, true, true));

            // Create the profile
            defaultProfile = new ClaimChunkWorldProfile(true, claimedChunks, unclaimedChunks);

            // Add default entity classes
            HashSet<EntityType> monsters = new HashSet<>();
            Arrays.stream(EntityType.values())
                    .filter(Objects::nonNull)
                    .filter(entityType
                            -> entityType.getEntityClass() != null
                            && Monster.class.isAssignableFrom(entityType.getEntityClass()))
                    .forEach(monsters::add);
            HashSet<EntityType> hangingEntities = new HashSet<>();
            Arrays.stream(EntityType.values())
                    .filter(Objects::nonNull)
                    .filter(entityType
                            -> entityType.getEntityClass() != null
                            && Hanging.class.isAssignableFrom(entityType.getEntityClass()))
                    .forEach(hangingEntities::add);
            HashSet<EntityType> animals = new HashSet<>();
            Arrays.stream(EntityType.values())
                    .filter(entityType
                            -> entityType.getEntityClass() != null
                            && Animals.class.isAssignableFrom(entityType.getEntityClass()))
                    .forEach(animals::add);
            defaultProfile.entityClasses.put("MONSTERS", monsters);
            defaultProfile.entityClasses.put("HANGING_ENTITIES", hangingEntities);
            defaultProfile.entityClasses.put("ANIMALS", animals);
        }
        return defaultProfile;
    }

}
