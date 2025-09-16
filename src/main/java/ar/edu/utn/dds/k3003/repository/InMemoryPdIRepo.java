package ar.edu.utn.dds.k3003.repository;

import ar.edu.utn.dds.k3003.model.PdI;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Profile("test")
public class InMemoryPdIRepo implements PdIRepository {
    private final Map<Long, PdI> diccionarioPdI = new HashMap<>();
    private final Map<String, List<PdI>> diccionarioHechos = new HashMap<>();

    public PdI save(PdI pdi) {
        diccionarioPdI.put(pdi.getId(), pdi);
        diccionarioHechos.computeIfAbsent(pdi.getHechoId(), k -> new ArrayList<>()).add(pdi);
        return pdi;
    }

    public Optional<PdI> findById(Long id) {
        return Optional.ofNullable(diccionarioPdI.get(id));
    }

    public List<PdI> findByHechoId(String hechoId) {
        return diccionarioHechos.getOrDefault(hechoId, new ArrayList<>());
    }

    @Override
    public List<PdI> findAll() {
        return new ArrayList<>(diccionarioPdI.values());
    }

    @Override
    public void deleteAll() {
        diccionarioPdI.clear();
        diccionarioHechos.clear();
    }
}
