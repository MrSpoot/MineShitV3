package com.mineshit.game.world;

public enum ChunkState {
    EMPTY,         // Pas encore généré
    GENERATED,     // Les blocs sont là
    MESHED,        // Mesh à jour
    DIRTY,          // Les données ont changé, il faut re-mesher
    DELETED
}

