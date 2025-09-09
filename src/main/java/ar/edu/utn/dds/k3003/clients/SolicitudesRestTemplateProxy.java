package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.facades.FachadaFuente;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.EstadoSolicitudBorradoEnum;
import ar.edu.utn.dds.k3003.facades.dtos.SolicitudDTO;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@Profile({"prod","default"})
public class SolicitudesRestTemplateProxy implements FachadaSolicitudes {

    private final RestTemplate rt;
    private final String base; // debe terminar en "/"

    public SolicitudesRestTemplateProxy(RestTemplate rt,
                                        @Value("${solicitudes.base-url}") String base) {
        this.rt = rt;
        this.base = base.endsWith("/") ? base : base + "/";
    }

    private record HechoResponseDTO(String hechoId, boolean activo) {}

    private String api(String path) {
        return base + (path.startsWith("/") ? path.substring(1) : path);
    }

    @Override
    public SolicitudDTO agregar(SolicitudDTO solicitudDTO) {
        ResponseEntity<SolicitudDTO> resp = rt.postForEntity(
                api("/api/solicitudes"), solicitudDTO, SolicitudDTO.class);
        return resp.getBody();
    }

    @Override
    public SolicitudDTO modificar(String id,
                                  EstadoSolicitudBorradoEnum estado, String motivo) throws NoSuchElementException {

        // Supuesto común: PUT/PATCH a /api/solicitudes/{id}
        var payload = Map.of("estado", estado, "motivo", motivo);
        var entity = new HttpEntity<>(payload, new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }});

        try {
            ResponseEntity<SolicitudDTO> resp = rt.exchange(
                    api("/api/solicitudes/" + id), HttpMethod.PATCH, entity, SolicitudDTO.class);
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoSuchElementException("No existe la solicitud " + id);
        }
    }

    @Override
    public List<SolicitudDTO> buscarSolicitudXHecho(String hechoId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(api("/api/solicitudes"))
                .queryParam("hecho", hechoId)
                .build().toUri();

        ResponseEntity<List<SolicitudDTO>> resp = rt.exchange(
                uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<SolicitudDTO>>() {});
        return resp.getBody();
    }

    @Override
    public SolicitudDTO buscarSolicitudXId(String id) {
        try {
            return rt.getForObject(api("/api/solicitudes/{id}"), SolicitudDTO.class, id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoSuchElementException("No existe la solicitud " + id);
        }
    }

    @Override
    public boolean estaActivo(String hechoId) {
        final String rel = "/solicitudes/hechos/{hechoId}"; // o "/api/solicitudes/hechos/{hechoId}" según tu base-url
        final URI uri = UriComponentsBuilder
                .fromHttpUrl(api(rel))        // api() concatena con la base normalizada
                .buildAndExpand(hechoId)      // expande la plantilla {hechoId}
                .toUri();

        // --- LOG de lo que envío ---
        log.info("[Solicitudes→GET] url={} hechoId={}", uri, hechoId);
        log.debug("[Solicitudes→GET] url={} hechoId={}", uri, hechoId);

        try {
            ResponseEntity<HechoResponseDTO> resp =
                    rt.getForEntity(uri, HechoResponseDTO.class);

            // --- LOG de lo que recibí ---
            log.info("[Solicitudes←RESP] status={} body={}",
                    resp.getStatusCodeValue(), resp.getBody());
            log.debug("[Solicitudes←RESP] status={} body={}",
                    resp.getStatusCodeValue(), resp.getBody());

            HechoResponseDTO body = resp.getBody();
            if (body == null) {
                log.info("[Solicitudes←RESP] cuerpo nulo para url={}", uri);
                log.error("[Solicitudes←RESP] cuerpo nulo para url={}", uri);
                throw new RestClientException("Respuesta vacía de Solicitudes para " + hechoId);
            }

            // --- LOG del valor interpretado que usarás en tu lógica ---
            log.info("[Solicitudes←OK] hechoId={} activo={}", body.hechoId(), body.activo());
            log.debug("[Solicitudes←OK] hechoId={} activo={}", body.hechoId(), body.activo());
            return body.activo();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[Solicitudes←ERR] 404 Not Found url={} hechoId={}", uri, hechoId);
            throw new NoSuchElementException("No existe el hecho " + hechoId + " en Solicitudes");
        } catch (RestClientException e) {
            log.error("[Solicitudes←ERR] Falló GET url={} hechoId={} msg={}",
                    uri, hechoId, e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public void setFachadaFuente(FachadaFuente fachadaFuente) {
        // si más adelante necesitás encadenar llamadas, inyectalo aquí
    }
}
