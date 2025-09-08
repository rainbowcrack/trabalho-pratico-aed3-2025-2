package br.com.mpet.storage;

import br.com.mpet.repository.*;

import java.io.IOException;

public class Maintenance {
    public static void vacuumAll(VoluntarioRepository v, AdotanteRepository a, OngRepository o, AnimalRepository an) throws IOException {
        // Not implemented per-repo, repositories would need exposure of index maps. Placeholder.
        // For now, just run vacuum on underlying files by reflection if accessible.
    }
}
