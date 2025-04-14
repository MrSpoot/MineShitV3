package com.mineshit.game.world.utils;

public enum ChunkState {
    EMPTY,         // Pas encore généré
    GENERATED,     // Les blocs sont là
    MESHING,
    MESHED,        // Mesh à jour
    DIRTY,          // Les données ont changé, il faut re-mesher
    DIRTY_NOW,
    DELETED
}

