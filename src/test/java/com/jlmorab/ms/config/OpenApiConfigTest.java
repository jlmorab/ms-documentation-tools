package com.jlmorab.ms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest
@ContextConfiguration( classes = {OpenApiConfig.class} )
class OpenApiConfigTest {
	
	private static final String ENDPOINT = "/test";
	
	@Autowired
	OpenApiCustomizer customizer;

	@Test
	void shouldAddGlobalResponsesToOperations() {
		OpenAPI openApi = new OpenAPI();
        Operation operation = new Operation().responses(new ApiResponses());
        PathItem pathItem = new PathItem().get( operation );
        openApi.setPaths(new Paths().addPathItem( ENDPOINT, pathItem ));
        
        customizer.customise(openApi);
        ApiResponses actual = openApi.getPaths().get( ENDPOINT ).getGet().getResponses();
        
        assertThat(actual).containsKeys("400", "404", "405", "500");
	}//end shouldAddGlobalResponsesToOperations()
	
	@Test
	void shouldMergeGlobalResponsesWithExistingResponses() {
		final String GENERIC_CONTENT = "generic-content";
		final String TEST_DESCRIPTION = "Existing";
		final String BAD_REQUEST = "400";
		final String WILDCARD = "*/*";
		
		OpenAPI openApi = new OpenAPI();

	    ApiResponse existing400 = new ApiResponse()
	        .description( TEST_DESCRIPTION )
	        .content(new Content()
	        		.addMediaType( WILDCARD,
	        				new MediaType().example(GENERIC_CONTENT)));

	    Operation getOp = new Operation()
	        .responses(new ApiResponses().addApiResponse( BAD_REQUEST, existing400 ));

	    PathItem pathItem = new PathItem().get(getOp);
	    openApi.setPaths(new Paths().addPathItem( ENDPOINT, pathItem));

	    customizer.customise(openApi);
	    
	    ApiResponses responses = openApi.getPaths().get( ENDPOINT ).getGet().getResponses();

	    assertThat(responses).containsKey( BAD_REQUEST );

	    ApiResponse merged400 = responses.get( BAD_REQUEST );
	    
	    assertThat(merged400.getDescription())
	        .contains( TEST_DESCRIPTION )
	        .contains("Solicitud inválida")
	        .contains("|");
	    
	    assertThat(merged400.getContent()).doesNotContainKey( WILDCARD );
	    assertThat(merged400.getContent()).containsKey("application/json");
	}//end shouldMergeGlobalResponsesWithExistingResponses()

}
