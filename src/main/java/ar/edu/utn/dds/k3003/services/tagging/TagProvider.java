package ar.edu.utn.dds.k3003.services.tagging;

import ar.edu.utn.dds.k3003.model.PdI;
import java.util.List;

public interface TagProvider {
    boolean supports(PdI pdi);               // p.ej., requiere imageUrl no vacía
    List<String> extractTags(PdI pdi) throws Exception;
    default String name() { return getClass().getSimpleName(); }
}