package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.repository.InMemoryPdIRepo;
import ar.edu.utn.dds.k3003.repository.PdIRepository;

import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Fachada implements FachadaProcesadorPDI {

    private FachadaSolicitudes fachadaSolicitudes;

    @Getter private final PdIRepository pdiRepository;

    private final AtomicLong generadorID = new AtomicLong(1);



    protected Fachada() {
        this.pdiRepository = new InMemoryPdIRepo();
    }

    @Autowired
    public Fachada(PdIRepository pdiRepository) {
        this.pdiRepository = pdiRepository;
    }

    @Override
    public void setFachadaSolicitudes(FachadaSolicitudes fachadaSolicitudes) {
        this.fachadaSolicitudes = fachadaSolicitudes;
    }

    @Override
    public PdIDTO procesar(PdIDTO pdiDTORecibido) {
        System.out.println("ProcesadorPdI.Fachada.procesar() recibió: " + pdiDTORecibido);

        final String hechoId = pdiDTORecibido.hechoId();

        log.info("[ProcesadorPdI] PROCESAR {})...", hechoId);

        PdI nuevoPdI = recibirPdIDTO(pdiDTORecibido);
        System.out.println("ProcesadorPdI.Fachada.procesar() mapeado a entidad: " + nuevoPdI);

        // Buscar duplicado a mano
        Optional<PdI> yaProcesado =
                pdiRepository.findByHechoId(nuevoPdI.getHechoId()).stream()
                        .filter(
                                p ->
                                        p.getDescripcion().equals(nuevoPdI.getDescripcion())
                                                && p.getLugar().equals(nuevoPdI.getLugar())
                                                && p.getMomento().equals(nuevoPdI.getMomento())
                                                && p.getContenido().equals(nuevoPdI.getContenido()))
                        .findFirst();

        if (yaProcesado.isPresent()) {
            return convertirADTO(yaProcesado.get());
        }

        nuevoPdI.setEtiquetas(etiquetar(nuevoPdI.getContenido()));
        pdiRepository.save(nuevoPdI);
        System.out.println("Guardado PdI id=" + nuevoPdI.getId() + " hechoId=" + nuevoPdI.getHechoId());


        System.out.println(
                "Se guardó el PdI con ID "
                        + nuevoPdI.getId()
                        + " en hechoId: "
                        + nuevoPdI.getHechoId());

        PdIDTO pdiDTOAEnviar = convertirADTO(nuevoPdI);

        System.out.println("ProcesadorPdI.Fachada.procesar() responde: " + pdiDTOAEnviar);

        return pdiDTOAEnviar;
    }

    @Override
    public PdIDTO buscarPdIPorId(String idString) {
        Long id = Long.parseLong(idString);
        PdI pdi =
                pdiRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "No se encontró el PdI con id: " + id));
        return convertirADTO(pdi);
    }

    @Override
    public List<PdIDTO> buscarPorHecho(String hechoId) {
        List<PdI> lista = pdiRepository.findByHechoId(hechoId);

        System.out.println("Buscando por hechoId: " + hechoId + " - Encontrados: " + lista.size());

        return lista.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    private PdIDTO convertirADTO(PdI p) {
        return new PdIDTO(
                p.getId() == null ? null : String.valueOf(p.getId()),
                p.getHechoId(),
                p.getDescripcion(),
                p.getLugar(),
                p.getMomento(),
                p.getContenido(),
                p.getEtiquetas(),
                p.getImageUrl()
        );
    }

    public List<String> etiquetar(String contenido) {
        List<String> etiquetas = new ArrayList<>();
        if (contenido != null) {
            if (contenido.toLowerCase().contains("fuego")) {
                etiquetas.add("incendio");
            }

            if (contenido.toLowerCase().contains("agua")) {
                etiquetas.add("inundación");
            }
        }
        if (etiquetas.isEmpty()) {
            etiquetas.add("sin clasificar");
        }
        return etiquetas;
    }

    private PdI recibirPdIDTO(PdIDTO d) {
        PdI p = new PdI();
        p.setHechoId(d.hechoId());
        p.setDescripcion(d.descripcion());
        p.setLugar(d.lugar());
        p.setMomento(d.momento());
        p.setContenido(d.contenido());
        p.setEtiquetas(d.etiquetas());
        p.setImageUrl(d.imageUrl());
        return p;
    }

        @Override
        public List<PdIDTO> pdis() {
            return this.pdiRepository.findAll()
                    .stream()
                    .map(this::convertirADTO)
                    .toList();
        }

        @Override
        public void borrarTodo() {
            pdiRepository.deleteAll();
            generadorID.set(1); // opcional: reiniciar IDs en memoria
        }
}
