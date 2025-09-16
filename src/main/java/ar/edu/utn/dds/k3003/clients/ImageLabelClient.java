package ar.edu.utn.dds.k3003.clients;

import java.util.List;

public interface ImageLabelClient {
    List<String> extractLabels(String imageUrl);
}

