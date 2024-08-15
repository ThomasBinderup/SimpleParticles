package plugin.simpleparticles;

import it.unimi.dsi.fastutil.Hash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public final class SimpleParticles extends JavaPlugin {
    private static SimpleParticles instance;
    private static SimpleParticles getInstance() { return instance;}

    private Map<UUID, TaskParticlePair> particleTasks = new HashMap<>();
    private class TaskParticlePair {
        private final BukkitTask task;
        private final Particle particle;

        public TaskParticlePair(BukkitTask task, Particle particle) {
            this.task = task;
            this.particle = particle;
        }

        public BukkitTask getTask() {
            return task;
        }

        public Particle getParticle() {
            return particle;
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("particles").setExecutor(new SimpleParticlesGUI());
        this.getCommand("particle").setExecutor(new SimpleParticlesGUI());
        this.getCommand("particles menu").setExecutor(new SimpleParticlesGUI());
        this.getCommand("particle menu").setExecutor(new SimpleParticlesGUI());
        getServer().getPluginManager().registerEvents(new SimpleParticlesMenu(), this);
        saveResource("config.yml", false);
        instance = this;
    }

    @Override
    public void onDisable() {

    }

    public HashMap<UUID, SimpleParticlesUtil> playerMenus = new HashMap<>();

    public class SimpleParticlesGUI implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (cmd.getPermission() == null) return false;
                if (cmd.getPermission().equals("simpleparticles.menu")) {
                    SimpleParticlesUtil simpleParticlesUtil = new SimpleParticlesUtil();
                    playerMenus.put(player.getUniqueId(), simpleParticlesUtil);
                    simpleParticlesUtil.openInventory(player);
                }
            }
            return true;
        }
    }

    HashMap<UUID, Material> hashmapEncMat = new HashMap<>();

    public class SimpleParticlesUtil {
        private final Inventory inv;
        private Inventory invParticles;

        public SimpleParticlesUtil() {
            inv = Bukkit.createInventory(null, 9, Component.text("SimpleParticles Menu").color(NamedTextColor.GOLD));

            initializeItems();
        }

        public void initializeItems() {
            inv.setItem(4, createGuiItem(Material.GLOWSTONE_DUST, "§6Particles", "§eClick to open particles"));
        }

        protected ItemStack createGuiItem(final Material material, final String name, final String... lore) {
            final ItemStack item = new ItemStack(material, 1);
            final ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);

            return item;
        }

        public void openInventory(final HumanEntity ent) {
            ent.openInventory(inv);
        }

        public void spawnParticlesAtPlayer(Player player, Particle particle, SimpleParticlesUtil.ParticleInfo particleInfo, ConfigurationSection particleConfig, int intervalTicks, ItemStack clickedItem) {
            TaskParticlePair taskParticlePair;
            Particle previousParticle;
            BukkitTask previousBukkitTask;

            if (particleTasks.get(player.getUniqueId()) != null) {
                taskParticlePair = particleTasks.get(player.getUniqueId());
                previousParticle = taskParticlePair.getParticle();
                previousBukkitTask = taskParticlePair.getTask();
                previousBukkitTask.cancel();

                ItemStack[] invParticleContents = invParticles.getContents();

                for (ItemStack item : invParticleContents) {
                    if (item != null && item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta.hasEnchant(Enchantment.UNBREAKING)) {
                            meta.removeEnchant(Enchantment.UNBREAKING);
                            item.setItemMeta(meta);
                            hashmapEncMat.put(player.getUniqueId(), null);
                        }
                    }
                }

                if (previousParticle.equals(particle)) {
                    particleTasks.put(player.getUniqueId(), null);
                    player.sendMessage(Component.text("Particle " + '"' + particleInfo.getParticleHeader(particle) + '"' + " was disabled!").color(NamedTextColor.RED));
                    return;
                }
            }

            BukkitTask bukkitTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (particleConfig == null) return;

                    int count = particleConfig.getInt("count");
                    double offsetX = particleConfig.getDouble("offsetX");
                    double offsetY = particleConfig.getDouble("offsetY");
                    double offsetZ = particleConfig.getDouble("offsetZ");

                    player.getWorld().spawnParticle(particle, player.getLocation(), count, offsetX, offsetY, offsetZ);
                }

            }.runTaskTimer(SimpleParticles.getInstance(), 0, intervalTicks);

            player.sendMessage(Component.text("Particle " + '"' + particleInfo.getParticleHeader(particle) + '"' + " was successfully enabled!").color(NamedTextColor.GREEN));

            ItemMeta meta = clickedItem.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            hashmapEncMat.put(player.getUniqueId(), clickedItem.getType());
            clickedItem.setItemMeta(meta);

            particleTasks.put(player.getUniqueId(), new TaskParticlePair(bukkitTask, particle));
        }

        static String usingCharacterToUpperCaseMethod(String input) {
            if (input == null || input.isEmpty()) {
                return null;
            }

            return Arrays.stream(input.split("\\s+"))
                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
        }

        public class ParticleInfo {
            public Material getParticleMaterial(Particle particle) {
                if (particle.equals(Particle.COMPOSTER)) return Material.COMPOSTER;
                if (particle.equals(Particle.ENCHANT)) return Material.ENCHANTING_TABLE;
                if (particle.equals(Particle.GLOW)) return Material.GLOW_INK_SAC;
                if (particle.equals(Particle.HAPPY_VILLAGER)) return Material.VILLAGER_SPAWN_EGG;
                if (particle.equals(Particle.HEART)) return Material.RED_DYE;
                if (particle.equals(Particle.LAVA)) return Material.LAVA_BUCKET;
                if (particle.equals(Particle.NAUTILUS)) return Material.NAUTILUS_SHELL;
                if (particle.equals(Particle.NOTE)) return Material.NOTE_BLOCK;
                if (particle.equals(Particle.PORTAL)) return Material.NETHERRACK;
                if (particle.equals(Particle.WAX_ON)) return Material.HONEYCOMB;

                return null;
            };

            public Particle getMaterialParticle(Material material) {
                if (material.equals(Material.COMPOSTER)) return Particle.COMPOSTER;
                if (material.equals(Material.ENCHANTING_TABLE)) return Particle.ENCHANT;
                if (material.equals(Material.GLOW_INK_SAC)) return Particle.GLOW;
                if (material.equals(Material.VILLAGER_SPAWN_EGG)) return Particle.HAPPY_VILLAGER;
                if (material.equals(Material.RED_DYE)) return Particle.HEART;
                if (material.equals(Material.LAVA_BUCKET)) return Particle.LAVA;
                if (material.equals(Material.NAUTILUS_SHELL)) return Particle.NAUTILUS;
                if (material.equals(Material.NOTE_BLOCK)) return Particle.NOTE;
                if (material.equals(Material.NETHERRACK)) return Particle.PORTAL;
                if (material.equals(Material.HONEYCOMB)) return Particle.WAX_ON;

                return null;
            }

            public String getParticleHeader(Particle particle) {
                String partString = particle.toString();
                char underscore = '_';
                char space = ' ';
                String replacedString = partString.replace(underscore, space).toLowerCase();

                return usingCharacterToUpperCaseMethod(replacedString);
            };

            public String getParticleDescription(Particle particle) {
                if (particle.equals(Particle.COMPOSTER)) {
                    return "Small sized green star-like particles";
                }
                if (particle.equals(Particle.ENCHANT)) {
                    return "Magical particles";
                }
                if (particle.equals(Particle.GLOW)) {
                    return "A beautiful blue glow";
                }
                if (particle.equals(Particle.HAPPY_VILLAGER)) {
                    return "Medium sized green star-like particles";
                }
                if (particle.equals(Particle.HEART)) {
                    return "Heart particles";
                }
                if (particle.equals(Particle.LAVA)) {
                    return "Lava particles";
                }
                if (particle.equals(Particle.NAUTILUS)) {
                    return "Nautilus shell particles";
                }
                if (particle.equals(Particle.NOTE)) {
                    return "Musical note particles";
                }
                if (particle.equals(Particle.PORTAL)) {
                    return "Swirling purple particles";
                }
                if (particle.equals(Particle.WAX_ON)) {
                    return "Orange star-like particles";
                }
                return "Unknown particle.";
            }
        }

        public void displayMenu(Material materialType, Player p) {
            ParticleInfo particleInfo = new ParticleInfo();
            if (materialType.equals(Material.GLOWSTONE_DUST)) {
                invParticles = Bukkit.createInventory(null, 36, Component.text("Particles").color(NamedTextColor.GOLD));
                Particle[] particles = {
                        Particle.COMPOSTER,
                        Particle.ENCHANT,
                        Particle.GLOW,
                        Particle.HAPPY_VILLAGER,
                        Particle.HEART,
                        Particle.LAVA,
                        Particle.NAUTILUS,
                        Particle.NOTE,
                        Particle.PORTAL,
                        Particle.WAX_ON
                };

                invParticles.setItem(27, createGuiItem(Material.ARROW, "§6Navigation", "§eGo back"));
                invParticles.setItem(10, createGuiItem(Material.BARRIER, "§4LOCKED", "§cYou currently don't have access to any particle effects."));
                int i = 0;
                for (Particle particle : particles) {
                    if (!p.hasPermission("simpleparticles.particles." + particle.toString().toLowerCase())) continue;

                    int b = 10 + i;
                    if (b == 17 || b == 18) b += 2;
                    if (b == 26) b++;


                    invParticles.setItem(b, createGuiItem(particleInfo.getParticleMaterial(particle), "§b" + particleInfo.getParticleHeader(particle), "§3" + particleInfo.getParticleDescription(particle)));
                    i++;

                    if (particleInfo.getParticleMaterial(particle).equals(hashmapEncMat.get(p.getUniqueId()))) {
                        ItemStack item = invParticles.getItem(b);
                        if (item == null) continue;
                        ItemMeta meta = item.getItemMeta();
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        item.setItemMeta(meta);
                    }
                }

                p.openInventory(invParticles);
            }
        }

    }

    public class SimpleParticlesMenu implements Listener {

        @EventHandler
        public void onInventoryClick(final InventoryClickEvent e) {
            TextComponent textComponent = (TextComponent) e.getView().title();
            if (!textComponent.content().equals("SimpleParticles Menu")) return;
            e.setCancelled(true);

            final ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) return;
            Material materialType = clickedItem.getType();

            Player p = (Player) e.getWhoClicked();

            SimpleParticlesUtil simpleParticlesUtil = playerMenus.get(p.getUniqueId());
            simpleParticlesUtil.displayMenu(materialType, p);
        }

        @EventHandler
        public void onParticleInventoryClick(final InventoryClickEvent e) {
            TextComponent textComponent = (TextComponent) e.getView().title();
            if (!textComponent.content().equals("Particles")) return;
            e.setCancelled(true);

            final ItemStack clickedItem = e.getCurrentItem();

            if (clickedItem == null || clickedItem.getType().isAir()) return;
            SimpleParticlesUtil simpleParticlesUtil = playerMenus.get(e.getWhoClicked().getUniqueId());
            SimpleParticlesUtil.ParticleInfo particleInfo = simpleParticlesUtil.new ParticleInfo();

            Material materialType = clickedItem.getType();

            if (materialType.equals(Material.ARROW)) {
                simpleParticlesUtil.openInventory(e.getWhoClicked());
                return;
            }
            FileConfiguration config = getInstance().getConfig();

            Particle particle = particleInfo.getMaterialParticle(materialType);
            String path = "particles." + particle.toString();
            ConfigurationSection particleConfig = config.getConfigurationSection(path);

            int intervalTicks = particleConfig.getInt("intervalTicks");

            simpleParticlesUtil.spawnParticlesAtPlayer((Player) e.getWhoClicked(), particleInfo.getMaterialParticle(materialType), particleInfo, particleConfig, intervalTicks, clickedItem);
        }

        @EventHandler
        public void onInventoryClick(final InventoryDragEvent e) {

        }


    }

}


