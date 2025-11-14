package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.clients.dtos.ProcesamientoFuentesDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class FuentesProxy {

    private final String endpoint;
    private final FuentesRetrofitClient service;

    private static final Logger log = LoggerFactory.getLogger(FuentesProxy.class);

    public FuentesProxy(ObjectMapper objectMapper) {
        var env = System.getenv();
        String base = env.getOrDefault("URL_FUENTES", "https://tp-anual-dds-fuentes.onrender.com/api/");
        this.endpoint = base.endsWith("/") ? base : base + "/";

        // evitar fallos por campos extras
        objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(this.endpoint)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        this.service = retrofit.create(FuentesRetrofitClient.class);

        log.info("[FuentesProxy] baseUrl={}", this.endpoint);
    }

    /**
     * Enviar procesamiento (etiquetas + pdiId) al Hecho del módulo Fuentes.
     */
    public ProcesamientoFuentesDTO enviarProcesamientoAHecho(
            String hechoId,
            ProcesamientoFuentesDTO body
    ) {
        long t0 = System.currentTimeMillis();
        log.info("[ProcesadorPdI → Fuentes] -> PATCH /api/hecho/{}/procesamiento body={}", hechoId, body);

        Response<ProcesamientoFuentesDTO> resp;

        try {
            resp = service.patchProcesamiento(hechoId, body).execute();
        } catch (Exception e) {
            long dt = System.currentTimeMillis() - t0;
            log.error("[ProcesadorPdI → Fuentes] IOException PATCH procesamiento id={} ({} ms): {}",
                    hechoId, dt, e.toString());
            throw new RuntimeException("Error de red llamando al servicio Fuentes", e);
        }

        long dt = System.currentTimeMillis() - t0;

        if (resp.isSuccessful()) {
            log.info("[ProcesadorPdI → Fuentes] <- {} ({} ms) id={}",
                    resp.code(), dt, hechoId);
            return resp.body();
        }

        if (resp.code() == 404) {
            log.warn("[ProcesadorPdI → Fuentes] <- 404 ({} ms) id={}", dt, hechoId);
            return null;
        }

        String err;
        try {
            err = resp.errorBody() != null ? resp.errorBody().string() : "<empty>";
        } catch (Exception ignore) {
            err = "<unreadable>";
        }

        log.error("[ProcesadorPdI → Fuentes] <- {} ({} ms) id={} error={}",
                resp.code(), dt, hechoId, err);

        throw new RuntimeException("Error desde Fuentes (status " + resp.code() + ")");
    }
}
