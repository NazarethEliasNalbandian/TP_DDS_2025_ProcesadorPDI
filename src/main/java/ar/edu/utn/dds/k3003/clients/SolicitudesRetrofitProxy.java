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
import org.springframework.context.annotation.Primary;
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
@Primary
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
        httpLog.setLevel(HttpLoggingInterceptor.Level.BODY);

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
        log.warn("[estaActivo] IN hechoId={}", hechoId); // visible aun con INFO

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Response<HechoResponseDTO> resp = api.getHechoActivo(hechoId).execute();
                int code = resp.code();
                log.warn("[estaActivo] attempt={} status={}", attempt, code);

                if (resp.isSuccessful()) {
                    HechoResponseDTO body = resp.body();
                    log.warn("[estaActivo] OK body={}", body);
                    return body != null && Boolean.TRUE.equals(body.activo());
                } else {
                    String err = resp.errorBody() != null ? resp.errorBody().string() : "(sin error body)";
                    log.error("[estaActivo] FAIL status={} errorBody={}", code, err);

                    if (code == 404) throw new NoSuchElementException("No existe el hecho " + hechoId);
                    if (code == 429 || code == 503) {
                        Thread.sleep(200L * (1L << (attempt - 1)));
                        continue;
                    }
                    throw new IOException("HTTP " + code + " " + err);
                }
            } catch (NoSuchElementException e) {
                log.error("[estaActivo] 404 hechoId={}", hechoId);
                throw e;
            } catch (Exception e) {
                log.error("[estaActivo] EXC intento={} hechoId={} msg={}", attempt, hechoId, e.getMessage(), e);
                if (attempt == 3) throw new RuntimeException("Fallo consultando Solicitudes", e);
                try { Thread.sleep(200L * (1L << (attempt - 1))); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Backoff interrumpido", ie);
                }
            }
        }
        // no debería llegar
        log.error("[estaActivo] OUT (sin resultado) hechoId={}", hechoId);
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
