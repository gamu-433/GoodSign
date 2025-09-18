package me.petoma21.goodSign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class GoodSignData {

    private final Location location;
    private final String title;
    private final String ownerName;
    private final String creator;
    private int likes;

    public GoodSignData(Location location, String title, String ownerName, String creator, int likes) {
        this.location = location;
        this.title = title;
        this.ownerName = ownerName;
        this.creator = creator;
        this.likes = likes;
    }

    public Location getLocation() {
        return location;
    }

    public String getTitle() {
        return title;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getCreator() {
        return creator;
    }

    public int getLikes() {
        return likes;
    }

    public void addLike() {
        likes++;
    }

    public void saveToConfig(ConfigurationSection config, String path) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getBlockX());
        config.set(path + ".y", location.getBlockY());
        config.set(path + ".z", location.getBlockZ());
        config.set(path + ".title", title);
        config.set(path + ".owner", ownerName);
        config.set(path + ".creator", creator);
        config.set(path + ".likes", likes);
    }

    public static GoodSignData fromConfig(ConfigurationSection config) {
        try {
            String worldName = config.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }

            int x = config.getInt("x");
            int y = config.getInt("y");
            int z = config.getInt("z");
            Location location = new Location(world, x, y, z);

            String title = config.getString("title");
            String owner = config.getString("owner");
            String creator = config.getString("creator");
            int likes = config.getInt("likes");

            return new GoodSignData(location, title, owner, creator, likes);
        } catch (Exception e) {
            return null;
        }
    }

    public String getLocationString() {
        return location.getWorld().getName() + " (" +
                location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ() + ")";
    }
}