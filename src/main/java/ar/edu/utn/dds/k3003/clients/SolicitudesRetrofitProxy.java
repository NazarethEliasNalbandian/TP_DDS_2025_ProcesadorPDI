package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.facades.FachadaFuente;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.EstadoSolicitudBorradoEnum;
import ar.edu.utn.dds.k3003.facades.dtos.SolicitudDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Proxy con Retrofit. Método clave: estaActivo(hechoId).
 */
@Slf4j
@Component
@Profile({"prod","default"})
public class SolicitudesRetrofitProxy implements FachadaSolicitudes {

    // ===== API =====
    interface SolicitudesApi {
        // OJO: si tu servicio real expone /api/solicitudes/... ajustá acá y/o en baseUrl
        @GET("solicitudes/hechos/{hechoId}")
        Call<HechoResponseDTO> getHechoActivo(@Path("hechoId") String hechoId);
    }

    // ===== DTO de respuesta del endpoint GET /solicitudes/hechos/{hechoId} =====
    public record HechoResponseDTO(
            @JsonAlias({"hechoId","id"}) String hechoId,
            Boolean activo
    ) { }

    private final SolicitudesApi api;

    public SolicitudesRetrofitProxy(@Value("${solicitudes.base-url}") String baseUrl) {
        // Normalizo baseUrl para que termine en "/"
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        // Logger HTTP (útil mientras depurás)
        HttpLoggingInterceptor httpLog = new HttpLoggingInterceptor(msg -> log.info("[HTTP] {}", msg));
        httpLog.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(30))
                .retryOnConnectionFailure(true)
                .addInterceptor(httpLog)
                .build();

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(base) // p.ej. https://dds-tp-anual-solicitudes.onrender.com/
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build();

        this.api = retrofit.create(SolicitudesApi.class);
    }

    // =========================
    // ÚNICO MÉTODO IMPORTANTE
    // =========================
    @Override
    public boolean estaActivo(String hechoId) {
        log.info("[Solicitudes→GET] hechoId={}", hechoId);

        // pequeño backoff defensivo ante 429/503 (hasta 3 intentos)
        int maxRetries = 3;
        long baseDelayMs = 250;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Response<HechoResponseDTO> resp = api.getHechoActivo(hechoId).execute();
                int code = resp.code();

                if (resp.isSuccessful()) {
                    HechoResponseDTO body = resp.body();
                    log.info("[Solicitudes←OK] status={} body={}", code, body);
                    if (body == null) {
                        throw new IOException("Respuesta sin cuerpo para hechoId=" + hechoId);
                    }
                    // true solo si vino explícitamente true
                    return Boolean.TRUE.equals(body.activo());
                }

                // 404 -> no existe el hecho en Solicitudes
                if (code == 404) {
                    log.warn("[Solicitudes←404] hechoId={}", hechoId);
                    throw new NoSuchElementException("No existe el hecho " + hechoId + " en Solicitudes");
                }

                // 429/503 -> retry con backoff y jitter
                if (code == 429 || code == 503) {
                    long wait = (long) (baseDelayMs * Math.pow(2, attempt - 1))
                            + ThreadLocalRandom.current().nextLong(50, 200);
                    log.warn("[Solicitudes←{}] intento={} waitMs={} hechoId={}", code, attempt, wait, hechoId);
                    Thread.sleep(wait);
                    continue;
                }

                // Otros códigos: error duro
                String errBody = resp.errorBody() != null ? resp.errorBody().string() : "(sin body)";
                throw new IOException("HTTP " + code + " al consultar Solicitudes: " + errBody);

            } catch (NoSuchElementException e404) {
                throw e404; // propago tal cual
            } catch (IOException e) {
                // error de red u otro problema de IO → reintento salvo último intento
                if (attempt == maxRetries) {
                    log.error("[Solicitudes←ERR] agotados reintentos hechoId={} msg={}", hechoId, e.getMessage());
                    throw new RuntimeException("Error al consultar Solicitudes para " + hechoId, e);
                }
                long wait = (long) (baseDelayMs * Math.pow(2, attempt - 1))
                        + ThreadLocalRandom.current().nextLong(50, 200);
                log.warn("[Solicitudes←IO] intento={} waitMs={} hechoId={} msg={}",
                        attempt, wait, hechoId, e.getMessage());
                try { Thread.sleep(wait); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Backoff interrumpido", ie);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Backoff interrumpido", ie);
            }
        }
        // no debería llegar
        return false;
    }

    // =========================
    // Stubs (no prioritarios)
    // =========================
    @Override
    public SolicitudDTO agregar(SolicitudDTO solicitudDTO) {
        throw new UnsupportedOperationException("No implementado en Retrofit proxy (enfocado en estaActivo).");
    }

    @Override
    public SolicitudDTO modificar(String id, EstadoSolicitudBorradoEnum estado, String motivo) {
        throw new UnsupportedOperationException("No implementado en Retrofit proxy (enfocado en estaActivo).");
    }

    @Override
    public List<SolicitudDTO> buscarSolicitudXHecho(String hechoId) {
        throw new UnsupportedOperationException("No implementado en Retrofit proxy (enfocado en estaActivo).");
    }

    @Override
    public SolicitudDTO buscarSolicitudXId(String id) {
        throw new UnsupportedOperationException("No implementado en Retrofit proxy (enfocado en estaActivo).");
    }

    @Override
    public void setFachadaFuente(FachadaFuente fachadaFuente) {
        // no-op
    }
}
