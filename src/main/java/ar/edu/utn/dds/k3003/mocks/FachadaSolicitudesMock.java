//package ar.edu.utn.dds.k3003.mocks;
//
//import ar.edu.utn.dds.k3003.facades.FachadaFuente;
//import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
//import ar.edu.utn.dds.k3003.facades.dtos.EstadoSolicitudBorradoEnum;
//import ar.edu.utn.dds.k3003.facades.dtos.SolicitudDTO;
//
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.NoSuchElementException;
//
//@Component
//@Profile("dev")
//public class FachadaSolicitudesMock implements FachadaSolicitudes {
//
//    @Override
//    public SolicitudDTO agregar(SolicitudDTO solicitudDTO) {
//        return null;
//    }
//
//    @Override
//    public SolicitudDTO modificar(
//            String s, EstadoSolicitudBorradoEnum estadoSolicitudBorradoEnum, String s1)
//            throws NoSuchElementException {
//        return null;
//    }
//
//    @Override
//    public List<SolicitudDTO> buscarSolicitudXHecho(String s) {
//        return List.of();
//    }
//
//    @Override
//    public SolicitudDTO buscarSolicitudXId(String s) {
//        return null;
//    }
//
//    @Override
//    public boolean estaActivo(String hechoId) {
//        return false;
//    }
//
//    @Override
//    public void setFachadaFuente(FachadaFuente fachadaFuente) {
//        // Vacío
//    }
//}
