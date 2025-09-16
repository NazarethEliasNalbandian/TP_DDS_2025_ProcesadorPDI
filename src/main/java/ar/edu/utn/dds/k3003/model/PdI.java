package ar.edu.utn.dds.k3003.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PdI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hechoId;
    private String descripcion;
    private String lugar;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime momento;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    private String contenido;

    @ElementCollection private List<String> etiquetas;

    // 🔽 Resultados del procesamiento de imagen
    @Lob
    private String ocrText;

    @ElementCollection
    @CollectionTable(name = "pdi_auto_tags", joinColumns = @JoinColumn(name = "pdi_id"))
    @Column(name = "tag")
    private List<String> autoTags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ProcessingState processingState = ProcessingState.PENDING;

    private String lastError;
    private LocalDateTime processedAt;

    public enum ProcessingState { PENDING, PROCESSING, PROCESSED, ERROR }

    public PdI(String hechoId, String descripcion, String lugar,
               LocalDateTime momento, String contenido, String imageUrl) {
        this.hechoId = hechoId;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.momento = momento;
        this.contenido = contenido;
        this.imageUrl = imageUrl; // 👈 nuevo
    }
}
