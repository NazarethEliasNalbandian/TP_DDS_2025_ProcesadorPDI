package ar.edu.utn.dds.k3003.facades;

import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import java.util.List;
import java.util.NoSuchElementException;

public interface FachadaProcesadorPDI {

    PdIDTO procesar(PdIDTO pdi) throws IllegalStateException;

    PdIDTO buscarPdIPorId(String pdiId) throws NoSuchElementException;

    List<PdIDTO> buscarPorHecho(String hechoId)
            throws NoSuchElementException;

    void setFachadaSolicitudes(FachadaSolicitudes fachadaSolicitudes);

    List<PdIDTO> pdis();

    void borrarTodo();



}