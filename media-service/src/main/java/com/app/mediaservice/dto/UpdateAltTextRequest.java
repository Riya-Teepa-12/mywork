package com.app.mediaservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAltTextRequest {
    @Size(max = 300)
    private String altText;
}
