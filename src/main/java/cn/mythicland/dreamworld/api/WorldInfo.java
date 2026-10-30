package cn.mythicland.dreamworld.api;

import cn.mythicland.dreamworld.WorldStatus;

/**
 * Immutable snapshot of a managed world's state.
 *
 * @param name   logical world name
 * @param status current lifecycle status
 * @param detail human-readable status detail, or an empty string
 */
public record WorldInfo(
        String name,
        WorldStatus status,
        String detail
) {

    public WorldInfo {
        detail = detail == null ? "" : detail;
    }
}
