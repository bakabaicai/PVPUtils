package com.pvp_utils.client.plugin;

final class PluginPlayerSession {
    private Object player;
    private Object level;
    private Object menu;
    private long generation;
    private long menuGeneration;

    void update(Object player, Object level, Object menu) {
        if (this.player != player || this.level != level) {
            this.player = player;
            this.level = level;
            generation++;
            this.menu = null;
            menuGeneration++;
        }
        if (this.menu != menu) {
            this.menu = menu;
            menuGeneration++;
        }
    }

    String playerToken() {
        return player == null || level == null ? null : Long.toString(generation);
    }

    boolean validPlayer(String token) {
        return token != null && token.equals(playerToken());
    }

    String menuToken() {
        return playerToken() == null || menu == null ? null : generation + ":" + menuGeneration;
    }

    boolean validMenu(String token) {
        return token != null && token.equals(menuToken());
    }

    void clear() {
        update(null, null, null);
    }
}
