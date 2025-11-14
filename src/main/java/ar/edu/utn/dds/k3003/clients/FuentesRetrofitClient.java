package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.clients.dtos.ProcesamientoFuentesDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface FuentesRetrofitClient {

    @PATCH("hecho/{id}/procesamiento")
    Call<ProcesamientoFuentesDTO> patchProcesamiento(
            @Path("id") String id,
            @Body ProcesamientoFuentesDTO body
    );
}
