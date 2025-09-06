package ar.edu.utn.dds.k3003.repository;

import ar.edu.utn.dds.k3003.model.PdI;

import java.util.List;
import java.util.Optional;

public interface PdIRepository {
    PdI save(PdI pdi);

    Optional<PdI> findById(Long id);

    List<PdI> findByHechoId(String hechoId);

    List<PdI> findAll();

    void deleteAll();
}
