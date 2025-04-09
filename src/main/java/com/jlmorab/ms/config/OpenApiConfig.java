package com.jlmorab.ms.config;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlmorab.ms.data.dto.MetaDTO;
import com.jlmorab.ms.data.dto.WebResponseDTO;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration
public class OpenApiConfig {
	
	@Bean
    OpenApiCustomizer globalResponseOpenApiCustomiser() {
		final String BAD_REQUEST 	= "400";
		final String NOT_FOUND 		= "404";
		final String NOT_ALLOWED 	= "405";
		final String INTERNAL_ERROR = "500";
		
		return openApi -> {
			Schema<?> webResponseSchema = new Schema<>().$ref("#/components/schemas/WebResponseDTO");

			Map<String, ApiResponse> globalResponses = Map.of(
					BAD_REQUEST, obtainOpenApiResponse( 
							webResponseSchema, HttpStatus.BAD_REQUEST, "Solicitud inválida" ),
					NOT_FOUND, obtainOpenApiResponse( 
							webResponseSchema, HttpStatus.NOT_FOUND, "Método no encontrado" ),
					NOT_ALLOWED, obtainOpenApiResponse( 
							webResponseSchema, HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido" ),
					INTERNAL_ERROR, obtainOpenApiResponse( 
							webResponseSchema, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor" )
            );
			
			openApi.getPaths().forEach((path, pathItem) ->
            	pathItem.readOperations().forEach(operation -> {
            		ApiResponses responses = operation.getResponses();
            		mergeApiResponses( responses, globalResponses );
            	})
            );
		};
    }//end globalResponseOpenApiCustomiser()
	
	private void mergeApiResponses( ApiResponses responses, Map<String, ApiResponse> globalResponses ) {
		globalResponses.forEach((code, globalResponse) -> {
			if( responses.containsKey( code ) ) {
				ApiResponse existing = responses.get( code );
				existing.setDescription( existing.getDescription() + " | " + globalResponse.getDescription());
				if( existing.getContent().containsKey( ALL_VALUE ) ) {
					existing.getContent().remove( ALL_VALUE );
				}//end if
				globalResponse.getContent().forEach( (mediaType, media) -> {
					if( !existing.getContent().containsKey(mediaType) ) {
						existing.getContent().addMediaType(mediaType, media);
					}//end if
				});
			} else {
				responses.addApiResponse(code, globalResponse);
			}//end if
		});
	}//end mergeApiResponses()
	
	private ApiResponse obtainOpenApiResponse( Schema<?> webResponseSchema, HttpStatus httpStatus, String message ) {
		StringBuilder jsonResponse = new StringBuilder();
		try {
			ObjectMapper mapper = new ObjectMapper();
			
			MetaDTO meta = new MetaDTO( httpStatus );
			WebResponseDTO response = WebResponseDTO.builder()
					.meta( meta )
					.build();
			
			jsonResponse.append( mapper.writeValueAsString( response ) );
		} catch( JsonProcessingException e ) {
			jsonResponse.append("{}");
		}//end try
		
		return new ApiResponse()
                .description( message )
                .content( new Content().addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema( webResponseSchema )
                                .example( jsonResponse.toString() )));
	}//end obtainOpenApiResponse()
	
}

