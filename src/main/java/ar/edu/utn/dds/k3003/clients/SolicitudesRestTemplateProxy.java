package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.facades.FachadaFuente;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.EstadoSolicitudBorradoEnum;
import ar.edu.utn.dds.k3003.facades.dtos.SolicitudDTO;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile({"prod","default"})
public class SolicitudesRestTemplateProxy implements FachadaSolicitudes {

    private final RestTemplate rt;
    private final String base; // debe terminar en "/"
    private static final Logger log = LoggerFactory.getLogger(SolicitudesRestTemplateProxy.class);


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
        final String rel = "/solicitudes/hechos/{hechoId}";
        final URI uri = UriComponentsBuilder
                .fromHttpUrl(api(rel))
                .buildAndExpand(hechoId)
                .toUri();

        log.info("[Solicitudes→GET] url={} hechoId={}", uri, hechoId);

        try {
            // pedir como String para loguear el raw body
            ResponseEntity<String> raw = rt.getForEntity(uri, String.class);
            log.info("[Solicitudes←RESP] status={} body={}", raw.getStatusCodeValue(), raw.getBody());

            if (!raw.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("Respuesta no exitosa: " + raw.getStatusCode());
            }

            // parsear al DTO con un ObjectMapper tolerante
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            HechoResponseDTO body = mapper.readValue(raw.getBody(), HechoResponseDTO.class);

            if (body == null) {
                log.error("[Solicitudes←RESP] cuerpo nulo para url={}", uri);
                throw new RestClientException("Respuesta vacía de Solicitudes para " + hechoId);
            }

            log.info("[Solicitudes←OK] hechoId={} activo={}", body.hechoId(), body.activo());

            // devolver true solo si vino explícitamente true
            return Boolean.TRUE.equals(body.activo());

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[Solicitudes←ERR] 404 Not Found url={} hechoId={}", uri, hechoId);
            throw new NoSuchElementException("No existe el hecho " + hechoId + " en Solicitudes");
        } catch (RestClientException | IOException e) {
            log.error("[Solicitudes←ERR] Falló GET url={} hechoId={} msg={}",
                    uri, hechoId, e.getMessage(), e);
            throw new RuntimeException("Error al consultar Solicitudes para " + hechoId, e);
        }
    }



    @Override
    public void setFachadaFuente(FachadaFuente fachadaFuente) {
        // si más adelante necesitás encadenar llamadas, inyectalo aquí
    }
}
